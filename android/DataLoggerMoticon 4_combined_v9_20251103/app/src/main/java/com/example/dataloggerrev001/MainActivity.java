package com.example.dataloggerrev001;

import java.io.BufferedWriter;

import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;



public class MainActivity extends AppCompatActivity {


    private LineGraphSeries<DataPoint> series1;
    private LineGraphSeries<DataPoint> series2;
    private LineGraphSeries<DataPoint> series3;
    private int lastX = 0;
    boolean sensorSelected;
    private SensorManager sensorManager;
    private BufferedWriter file;
    private LocationManager locationManager;
    private Map<Integer, String> sensorTypes = new HashMap<Integer, String>();
    private Map<Integer, Sensor> sensors = new HashMap<Integer, Sensor>();
    private TextView filenameDisplay;
    private TextView logDisplay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Get sensors to be captured
        sensorTypes.put(Sensor.TYPE_ACCELEROMETER, "ACCEL");
//        sensorTypes.put(Sensor.TYPE_GYROSCOPE, "GYRO");
//        sensorTypes.put(Sensor.TYPE_LINEAR_ACCELERATION, "LINEAR");
//        sensorTypes.put(Sensor.TYPE_MAGNETIC_FIELD, "MAG");
//        sensorTypes.put(Sensor.TYPE_GRAVITY, "GRAV");
//        sensorTypes.put(Sensor.TYPE_ROTATION_VECTOR, "ROTATION");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        for (Integer type : sensorTypes.keySet()) {
            sensors.put(type, sensorManager.getDefaultSensor(type));
        }

        //LocationPermission
        locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);


        // Register click listeners for buttons and check box
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("SYNC APP");

        Button button1 = findViewById(R.id.button1);
        Button button2 = findViewById(R.id.button2);
        Button buttonMoticon = findViewById(R.id.buttonMoticon);
        Button buttonXsensDot = findViewById(R.id.buttonXsensDot);
        CheckBox checkbox1 = findViewById(R.id.checkBox1);
        checkbox1.setOnClickListener(clickListener);



        filenameDisplay = (TextView) findViewById(R.id.filename);
        logDisplay = (TextView) findViewById(R.id.log);

        // graph view instance
        GraphView graph = (GraphView) findViewById(R.id.graph);
        //data
        series1 = new LineGraphSeries<DataPoint>();
        series2 = new LineGraphSeries<DataPoint>();
        series3 = new LineGraphSeries<DataPoint>();
        series1.setColor(Color.BLUE);
        series2.setColor(Color.RED);
        series3.setColor(Color.YELLOW);
        graph.addSeries(series1);
        graph.addSeries(series2);
        graph.addSeries(series3);
        //custom viewport
        Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMaxY(40);
        viewport.setMinY(-40);
//        viewport.setMaxX(20);
        viewport.setMinX(0);
        viewport.setScrollable(true);
        graph.onDataChanged(true, false);

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopRecording();
            }
        });

    }

    public void jumpToMoticon(View view) {
        Intent intent = new Intent(MainActivity.this, MoticonActivity.class);
        startActivity(intent);
    }


    public void jumpToXsnesDot(View view) {
        Intent intent = new Intent(MainActivity.this, XsensDotActivity.class);
        startActivity(intent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
//        stopRecording();
    }


    private void startRecording() {
        // Prepare data storage
        File directory = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        SimpleDateFormat sdf = new SimpleDateFormat("MMMddyyyyHHmm");
        Date currentdate = new Date(System.currentTimeMillis());
//        String name = "AllData_" + sdf.format(currentdate) + ".csv";
        String name = "AllData_" + sdf.format(currentdate) + ".dat";
        File filename = new File(directory, name);
        try {
            file = new BufferedWriter(new FileWriter(filename));

        } catch (IOException e) {
            e.printStackTrace();
        }
        filenameDisplay.setText(name);

        int delay = 10000;
        // Register sensor listeners
        for (Sensor sensor : sensors.values()) {
            sensorManager.registerListener(sensorListener, sensor,
                    delay);
//            SensorManager.SENSOR_DELAY_GAME
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "First enable LOCATION ACCESS in settings.", Toast.LENGTH_LONG).show();
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                0, locationListener);
    }

    private void stopRecording() {
        sensorManager.unregisterListener(sensorListener);
        locationManager.removeUpdates(locationListener);
        filenameDisplay.setText("");
        try {
            file.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private OnClickListener clickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.checkBox1) {
                if (((CheckBox) v).isChecked()) {
                    sensorSelected = true;
                } else {
                    sensorSelected = false;
                }

            }
        }
    };
        private SensorEventListener sensorListener = new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                write(sensorTypes.get(event.sensor.getType()), event.values);
                if ("ACCEL".equals(sensorTypes.get(event.sensor.getType()))) {
                    //add data to graph and display max 20 points on the viewport
//                Log.d("Info","acc"+ event.values[0]);
                    series1.appendData(new DataPoint(lastX++, event.values[0]), false, 20);
                    series2.appendData(new DataPoint(lastX++, event.values[1]), false, 20);
                    series3.appendData(new DataPoint(lastX++, event.values[2]), false, 20);
                }
            }

        };

        private LocationListener locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                write("GPS",
                        new double[]{location.getLatitude(),
                                location.getLongitude()});
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

        };

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }

        private void write(String tag, String[] values) {
            if (file == null) {
                return;
            }

            String line = "";
            if (values != null) {
                for (String value : values) {
                    line += "," + value;
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("MMMddyyyyHHmm");
            Date currentdate = new Date(System.currentTimeMillis());
            Long tsLong = System.currentTimeMillis();
            line = tsLong + "," + tag + line + "\n";
//        line = sdf.format(currentdate) + "," + tag + line
//                + "\n";

            try {

                file.write(line);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            logDisplay.setText(line);
        }

        private void write(String tag, float[] values) {
            String[] array = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                array[i] = Float.toString(values[i]);
            }
            write(tag, array);
        }

        private void write(String tag, double[] values) {
            String[] array = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                array[i] = Double.toString(values[i]);
            }
            write(tag, array);
        }

        private void write(String tag) {
            write(tag, (String[]) null);
        }

    }