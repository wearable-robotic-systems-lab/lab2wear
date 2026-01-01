package com.example.dataloggerrev001;
import static android.media.AudioTimestamp.TIMEBASE_BOOTTIME;
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import java.util.Iterator;
import java.util.List;

import android.Manifest;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.google.protobuf.InvalidProtocolBufferException;

import de.moticon.insole3_service.Insole3Service;
import de.moticon.insole3_service.proto_mobile.Service;
import de.moticon.insole3_service.proto_mobile.Common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MoticonActivity extends AppCompatActivity{
    public boolean mBound = false;
    public boolean standalone = true;
    public boolean BLE_status = false;
    public int REQUEST_ENABLE_BT = 1;
    public int sensorLeft = 0;
    public int sensorRight = 0;
    public int leftZeroSuccess = 0;
    public int rightZeroSuccess = 0;
    public long MoticonTime;
    public float[] MoticonACC_Left = new float[3];
    public float[] MoticonACC_Right = new float[3];
    public float[] MoticonAngular_Left = new float[3];
    public float[] MoticonAngular_Right = new float[3];
    public int[] MoticonPressure_Left = new int[16];
    public int[] MoticonPressure_Right = new int[16];
    public float MoticonTemp_Left;
    public float MoticonTemp_Right;
    public int MoticonTotalForce_Left;
    public int MoticonTotalForce_Right;
    public float[] MoticonCOP_Left = new float[2];
    public float[] MoticonCOP_Right = new float[2];
    public long MoticonReceivedTime;
    public int DataSide = 2;
    public boolean ZeroingComplete = false;
    public BluetoothLeScanner bluetoothLeScanner;
    HandlerThread handlerThread_P2P = new HandlerThread("P2P");
    HandlerThread handlerThread_Sensor = new HandlerThread("Sensor");
    public List<Service.InsoleDevice> InsoleDeviceList = new ArrayList<>();
    public List<String> InsoleAddressList = new ArrayList<>();
    public List<Integer> insoleSerialNumbers = new ArrayList<>(Arrays.asList(0, 0));
    public static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    public static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 2;
    public static final int MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 3;
    public static final int MY_PERMISSIONS_REQUEST_BLUETOOTH = 4;
    public static final int MY_PERMISSIONS_REQUEST_CONNECT = 5;
    public static final int MY_PERMISSIONS_REQUEST_SCAN = 6;
    public static final int MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES = 7;
    LocationManager locationManager;
    Insole3Service.Insole3Binder mInsole3Service;
    BroadcastReceiver br = createServiceMsgReceiver();

    Button buttonSTSP;
    TextView SBCStatusText;
    TextView MoticonStatusText;
    TextView statusBar;
    ProgressBar progressBar_SBC;
    ImageView greencheck_SBC;

    ProgressBar progressBar_Moticon;
    ImageView greencheck_Moticon;

    String deviceName_SBC = "RPi_SyncModule";
    String deviceMACAddress_SBC;
    String ipAddress_SBC = "192.168.49.2"; // Static IP from the client
    int port_SBC = 8810;

    boolean isFound_SBC = false;
    boolean wifiDirectConnected_SBC = false;
    boolean isConnected_SBC = false;
    boolean clientObjectCreated_SBC = false;
    boolean isConnected_all = false;
    boolean wifiDirectConnected_all = false;

    boolean startRecording = false;
    boolean isRecording_SBC = false;
    boolean isRecording_all = false;

    boolean powerOffSystem = false;
    boolean isOff_SBC = false;

    ClientCommunication clientObject_SBC;

    // FOR CRISTIAN'S FORMULA
    public long T1_SBC = 0, T2_SBC = 0, T3_SBC = 0, T4_SBC = 0;

    public long startReceive = 0;
    public long endReceive = 0;
    public long startSend = 0;
    public long endSend = 0;
    public long receiveRate = 0;
    public long sendRate = 0;
    public long sessionTime = 0;
    long sessionStartTime = 0;
    long currentTime = 0;
    int PACKET_LENGTH = 47;
    int PACKET_NUMBER_Phone = 0;
    int PACKET_NUMBER_Moticon = 0;
    int LOG_PACKET_LENGTH = 58;
    int LOG_PACKET_LENGTH_MOTICON = 234;
    int cmdIndex = 3;
    boolean waitFlag = true;
    String dataFileName;
    String MoticonFileName;


    // FOR WIFI DIRECT
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter_P2P;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    // FOR PHONE-EMBEDDED SENSORS
    SensorManager mSensorManager;
    Sensor mAccelerometer;
    TextView sensorReading_Phone;
    TextView sensorReading_Moticon;

    // FOR DATA FILE
    public File dataFile;
    public File MoticonFile;
    float[] sensorVals = new float[3];
    byte[] LogPacket;
    byte[] LogPacket_Moticon;
    public byte[] COMBINED_PACKET_PHONE1 = new byte[2097106];//580//1048524//2097106//5242852
    public byte[] COMBINED_PACKET_PHONE2 = new byte[2097106];//580//1048524//2097106//5242852
    public byte[] COMBINED_PACKET_MOTICON1 = new byte[2133612];//2300//1048570//2097140//5242850
    public byte[] COMBINED_PACKET_MOTICON2 = new byte[2133612];//2300//1048570//2097140//5242850

//    public ByteBuffer buffer_phone1 = ByteBuffer.wrap(COMBINED_PACKET_PHONE1);
//    public ByteBuffer buffer_phone2 = ByteBuffer.wrap(COMBINED_PACKET_PHONE2);
//    public ByteBuffer buffer_moticon1 = ByteBuffer.wrap(COMBINED_PACKET_MOTICON1);
//    public ByteBuffer buffer_moticon2 = ByteBuffer.wrap(COMBINED_PACKET_MOTICON2);

    //PLAY SOUND
    public MediaPlayer mP;
    public final float ACCELERATION_THRESHOLD = 142.0f; //158*0.9

    //Double buffer for writing data
    public ExecutorService executorRecv_WritePhone = Executors.newSingleThreadExecutor();
    public ExecutorService executorRecv_WriteMoticon = Executors.newSingleThreadExecutor();
    public int CURR_PACKET_PHONE = 1;
    public int CURR_PACKET_MOTICON = 1;
    public boolean PACKET_PHONE1_STATUS = false;
    public boolean PACKET_PHONE2_STATUS = false;
    public boolean PACKET_MOTICON1_STATUS = false;
    public boolean PACKET_MOTICON2_STATUS = false;

    int audioSource = MediaRecorder.AudioSource.UNPROCESSED;
    int sampleRateInHz = 11025;//11025
    int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

    AudioRecord audioRecord = null;
    public boolean AudioStart = true; //false;
    public boolean audioTimeCatch = false;
    public long audioEnableTime=0;
    public long accEnableTime = 0;
    public long audioStartTimeSys = 0;
    public long audioStartTimeSys_before = 0;
    public long audioStartTimeSys_after = 0;
    public long audioStartTimeSys_period = 0;
    public long audioStartTimeFrame;
    public long audioStartTimeRec = 0;
    public long Audiotime=0;
    public byte[] LogPacket_AudioTime = new byte[22];
    public Thread recordingThread= null;
    Button button_Audio;
    public File AudioFile;
    public File AudioTimeFile;
    public int dataPoint=0;
    public int AudioITE = 0;

    public long MoticonStartTime = 0;
    public long MoticonServiceStartTime = 0;

    int leftConfigSuccess = 0;
    int rightConfigSuccess = 0;


    public ExecutorService executorRecv_WriteAudio = Executors.newSingleThreadExecutor();
    public ExecutorService executorRecv_WriteAudioTime = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moticon_3);
        initiatePermission();
        checkLowLatencySupport();
        bindService(this, standalone);
        //register broadcast receiver - Moticon
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(br, new IntentFilter(Insole3Service.BROADCAST_SERVICE_MSG));
        //register broadcaast receiver- P2P
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        intentFilter_P2P = new IntentFilter();
        intentFilter_P2P.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter_P2P.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter_P2P.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter_P2P.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        handlerThread_P2P.start();
        Looper looper_P2P = handlerThread_P2P.getLooper();
        Handler handler_P2P = new Handler(looper_P2P);
        registerReceiver(receiver, intentFilter_P2P, null, handler_P2P);

        //register sensor listener
        handlerThread_Sensor.start();
        Looper looper_Sensor = handlerThread_Sensor.getLooper();
        Handler handler_Sensor = new Handler(looper_Sensor);



        init(handler_Sensor);
        Button button_Moticon = findViewById(R.id.buttonMoticon);
        button_Moticon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                scanAndConnection();
                button_Moticon.setEnabled(false);
            }
        });

//        button_Audio = (Button) findViewById(R.id.buttonAudio);
//        button_Audio.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                if (!AudioStart){
//                    Log.i("Info","Audio start!!!");
//                    audioEnableTime = (SystemClock.elapsedRealtimeNanos() - sessionStartTime) / 1000;
//                    AudioStart=true;
//                    audioTimeCatch=true;
//                    dataPoint=0;
//                    AudioITE=0;
//                    initAudio();
//                    startAudio();
//                    CURR_PACKET_PHONE=1;
//                    button_Audio.setText("STOP Audio");
//
//                } else{
//                    Log.i("Info","Audio stop!!!");
//                    AudioStart=false;
//                    stopAudio();
//                    button_Audio.setText("START Audio");
//
//                }
//            }
//        });

    }
    private void checkLowLatencySupport() {
        boolean hasLowLatencyFeature = getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY);

        if (hasLowLatencyFeature) {
            // Inform the user that the device supports low latency audio
            Toast.makeText(this, "Low latency audio is supported!", Toast.LENGTH_SHORT).show();
        } else {
            // Inform the user that the device does not support low latency audio
            Toast.makeText(this, "Low latency audio is not supported.", Toast.LENGTH_SHORT).show();
        }
    }

    public void initAudio(){
        Log.i("Info", "min buffer size: " + bufferSizeInBytes);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            Log.i("Info","NO AUDIO PERMISSION");
            return;
        }
        audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, 663*2);
    }

    public void startAudio(){
        Audiotime = System.currentTimeMillis()/ 1000L;

        String AudioFileName = sessionTime + "_" + Audiotime+"_MIC.pcm";
        AudioFile = new File(getExternalFilesDir(null),"/LogFiles/" + AudioFileName);
        File dataDirectory = new File(getExternalFilesDir(null),"/LogFiles/");
        if(!dataDirectory.exists()) {
            dataDirectory.mkdirs();
        }
        if(!AudioFile.exists()) {
            try {
                AudioFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String AudioTimeFileName = sessionTime + "_" + Audiotime+"_MIC_Time.dat";
        AudioTimeFile = new File(getExternalFilesDir(null),"/LogFiles/" + AudioTimeFileName);
        if(!AudioTimeFile.exists()) {
            try {
                AudioTimeFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        audioRecord.startRecording();
        recordingThread = new Thread(new Runnable() {
            public void run() {
//                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    public void stopAudio() {
        // stops the recording activity
        if (null != audioRecord) {
            audioRecord.stop();
            audioRecord.release();
            recordingThread = null;
            audioRecord = null;
        }
    }


    public byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    public void writeAudioDataToFile(){
        while (AudioStart) {
            short[] sData = new short[bufferSizeInBytes/32];
            int length = audioRecord.read(sData, 0, bufferSizeInBytes/32, AudioRecord.READ_BLOCKING);
            audioStartTimeSys_after = (SystemClock.elapsedRealtimeNanos() - sessionStartTime);
            AudioTimestamp StopTS = new AudioTimestamp();
            audioRecord.getTimestamp(StopTS,TIMEBASE_BOOTTIME);
            long audioFrame = StopTS.framePosition;
            byte[] bData = short2byte(sData);
            dataPoint = dataPoint + 1;
            AudioITE = AudioITE + 1;
            LogPacket_AudioTime = createLogPacket_AudioTime_rev002(audioStartTimeSys_after, audioFrame);
            if (length>0) {

//            executorRecv_WriteAudioTime.submit(new Runnable() {
//                @Override
//                public void run() {
                try (FileOutputStream fileOut = new FileOutputStream(AudioTimeFile, true)) {
                    Log.i("TAG", "length: " +length);
                    fileOut.write(LogPacket_AudioTime);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Arrays.fill(LogPacket_AudioTime, (byte) 0);

//                }
//            });

//            executorRecv_WriteAudio.submit(new Runnable() {
//                @Override
//                public void run() {
                try (FileOutputStream fileOut = new FileOutputStream(AudioFile, true)) {
                    fileOut.write(bData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Arrays.fill(bData, (byte) 0);
//                }
//            });
            }


            audioStartTimeSys_period = audioStartTimeSys_after - audioStartTimeSys_before;
            if (audioTimeCatch) {
                audioStartTimeSys = audioStartTimeSys_after;
                audioTimeCatch = false;
                int SampleRate = audioRecord.getSampleRate();
                Log.i("TAG","SampleRate is: "+SampleRate );
            }
        }


    }



    public void addPackets_Phone(int PACKET_ORDER,byte[] COMBINED_PACKET,byte[]LogPacket){
        int start = (PACKET_ORDER-1) * LOG_PACKET_LENGTH;
        System.arraycopy(LogPacket,0,COMBINED_PACKET,start,LOG_PACKET_LENGTH);
    }
    public void addPackets_Moticon(int PACKET_ORDER,byte[] COMBINED_PACKET,byte[]LogPacket){
        int start = (PACKET_ORDER-1) * LOG_PACKET_LENGTH_MOTICON;
        System.arraycopy(LogPacket,0,COMBINED_PACKET,start,LOG_PACKET_LENGTH_MOTICON);
    }

    public void writeSensorDataToFile(){
            if (isRecording_all && AudioStart) {
                if (CURR_PACKET_PHONE == 1) {

                    try (FileOutputStream fileOut = new FileOutputStream(dataFile, true)) {
                        fileOut.write(COMBINED_PACKET_PHONE2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    PACKET_PHONE2_STATUS = false;
                    Arrays.fill(COMBINED_PACKET_PHONE2, (byte) 0);
                } else if (CURR_PACKET_PHONE == 2) {
                    try (FileOutputStream fileOut = new FileOutputStream(dataFile, true)) {
                        fileOut.write( COMBINED_PACKET_PHONE1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    PACKET_PHONE1_STATUS = false;
                    Arrays.fill(COMBINED_PACKET_PHONE1, (byte) 0);
                }
            } else if(isRecording_all && ! AudioStart && COMBINED_PACKET_PHONE1[0]==1){
                try (FileOutputStream fileOut = new FileOutputStream(dataFile, true)) {
                    fileOut.write(COMBINED_PACKET_PHONE1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Arrays.fill(COMBINED_PACKET_PHONE1, (byte) 0);
            }else if(isRecording_all && ! AudioStart && COMBINED_PACKET_PHONE2[0]==1){
                try (FileOutputStream fileOut = new FileOutputStream(dataFile, true)) {
                    fileOut.write(COMBINED_PACKET_PHONE2);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Arrays.fill(COMBINED_PACKET_PHONE2, (byte) 0);
            }
    }

    public void writeMoticonDataToFile(){
        if (isRecording_all) {
            if (CURR_PACKET_MOTICON == 1 ) {

                try (FileOutputStream fileOut = new FileOutputStream(MoticonFile, true)) {
                    fileOut.write(COMBINED_PACKET_MOTICON2);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                PACKET_MOTICON2_STATUS = false;
                Arrays.fill(COMBINED_PACKET_MOTICON2, (byte) 0);
            } else if (CURR_PACKET_MOTICON == 2) {
                try (FileOutputStream fileOut = new FileOutputStream(MoticonFile, true)) {
                    fileOut.write( COMBINED_PACKET_MOTICON1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                PACKET_MOTICON1_STATUS = false;
                Arrays.fill(COMBINED_PACKET_MOTICON1, (byte) 0);
            }
        }else if(!isRecording_all && COMBINED_PACKET_MOTICON1[0]==1){
            try (FileOutputStream fileOut = new FileOutputStream(MoticonFile, true)) {
                fileOut.write(COMBINED_PACKET_MOTICON1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Arrays.fill(COMBINED_PACKET_MOTICON1, (byte) 0);
        }else if(!isRecording_all && COMBINED_PACKET_MOTICON2[0]==1){
            try (FileOutputStream fileOut = new FileOutputStream(MoticonFile, true)) {
                fileOut.write(COMBINED_PACKET_MOTICON2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Arrays.fill(COMBINED_PACKET_MOTICON2, (byte) 0);
        }
    }


    private final SensorEventListener sensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sensorReading_Phone.setText(Float.toString(sensorEvent.values[2]));
                }
            });
            // Store sensor changes
//        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorVals[0] = sensorEvent.values[0];
            sensorVals[1] = sensorEvent.values[1];
            sensorVals[2] = sensorEvent.values[2];
            if (sensorVals[0] > ACCELERATION_THRESHOLD || sensorVals[1] > ACCELERATION_THRESHOLD || sensorVals[2] > ACCELERATION_THRESHOLD) {
                if (mP != null) {
                    mP.release();
                }
                mP = MediaPlayer.create(MoticonActivity.this, Settings.System.DEFAULT_NOTIFICATION_URI);
                mP.start();
                Handler handler_sound = new Handler();
                handler_sound.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mP.stop();
                    }
                }, 2 * 1000);
            }


            if (isRecording_all && AudioStart) {
                currentTime = (SystemClock.elapsedRealtimeNanos() - sessionStartTime) / 1000; // In micros
//            currentTime = SystemClock.elapsedRealtimeNanos() / 1000; // In micros
//                LogPacket = createLogPacket(sensorVals, currentTime, 0, 0, 0, 0);

                LogPacket = createLogPacket(sensorVals, currentTime,audioEnableTime, audioStartTimeSys,audioStartTimeRec,audioStartTimeFrame);
                PACKET_NUMBER_Phone = PACKET_NUMBER_Phone + 1;
                if (CURR_PACKET_PHONE==1){
                    addPackets_Phone(PACKET_NUMBER_Phone,COMBINED_PACKET_PHONE1,LogPacket);
//                    buffer_phone1.put(LogPacket);
                }else if (CURR_PACKET_PHONE==2){
                    addPackets_Phone(PACKET_NUMBER_Phone,COMBINED_PACKET_PHONE2,LogPacket);
//                buffer_phone2.put(LogPacket);
                }
                if (PACKET_NUMBER_Phone == 36157) {
//                    COMBINED_PACKET = buffer_phone.array();
//                    try (FileOutputStream fileOut = new FileOutputStream(dataFile, true)) {
//                        fileOut.write(COMBINED_PACKET);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                    if (CURR_PACKET_PHONE==1){
//                        COMBINED_PACKET_PHONE1 = buffer_phone1.array();
                        PACKET_PHONE1_STATUS = true;
                        CURR_PACKET_PHONE=2;
//                        buffer_phone1.clear();
                    }else if (CURR_PACKET_PHONE==2){
//                        COMBINED_PACKET_PHONE2 = buffer_phone2.array();
                        PACKET_PHONE2_STATUS = true;
                        CURR_PACKET_PHONE=1;
//                        buffer_phone2.clear();

                    }

                    executorRecv_WritePhone.submit(new Runnable(){
                        @Override
                        public void run(){
                            writeSensorDataToFile();
                        }
                    });

//                    buffer_phone.clear();
//                    Arrays.fill(COMBINED_PACKET, (byte) 0);
                    PACKET_NUMBER_Phone = 0;
                }
            }else if(isRecording_all && !AudioStart && (COMBINED_PACKET_PHONE1[0]==1 || COMBINED_PACKET_PHONE2[0]==1)) {
//            }else if(isRecording_all  && (COMBINED_PACKET_PHONE1[0]==1 || COMBINED_PACKET_PHONE2[0]==1)) {
                if (COMBINED_PACKET_PHONE1[0]==1 && COMBINED_PACKET_PHONE2[0]==1){
                    Log.i("Info","package error!");
                }
                Log.i("Info","writing last phone buffer");
                executorRecv_WritePhone.submit(new Runnable(){
                    @Override
                    public void run(){
                        writeSensorDataToFile();
                        PACKET_NUMBER_Phone = 0;
                    }
                });
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void init(Handler handler_Sensor) {
        // INITIALIZE VIEW COMPONENTS
        buttonSTSP = (Button) findViewById(R.id.buttonSTSP);
        statusBar = (TextView) findViewById(R.id.statusBar);
        SBCStatusText = (TextView) findViewById(R.id.statusText_SBC);
        MoticonStatusText = (TextView) findViewById(R.id.statusText_Moticon);
        progressBar_SBC = (ProgressBar) findViewById(R.id.progressBar_SBC);
        greencheck_SBC = (ImageView) findViewById(R.id.greenCheck_SBC);
        progressBar_Moticon = (ProgressBar) findViewById(R.id.progressBar_Moticon);
        greencheck_Moticon = (ImageView) findViewById(R.id.greenCheck_Moticon);
        buttonSTSP.setEnabled(false);

        // INITIALIZE SENSOR MANAGER
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // If phone lacks this sensor, set to -1
        if (mAccelerometer == null) {
            Log.e("Accelerometer", "Accelerometer not supported on this device");
            sensorVals[0] = -1;
            sensorVals[1] = -1;
            sensorVals[2] = -1;
        } else {
            Log.e("Accelerometer", "Accelerometer found!");
            mSensorManager.registerListener(sensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST,handler_Sensor);
        }
        sensorReading_Phone = (TextView) findViewById(R.id.sensorData_Phone);
        sensorReading_Moticon = (TextView) findViewById(R.id.sensorData_Moticon);

        // REMOVE EXISTING GROUP
        removeExistingGroup();

        // START WIFI P2P DISCOVERY
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("WifiDirect", "Discovery started!");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.e("WifiDirect", "Discovery failed!: " + reasonCode);
            }
        });


        // THREAD TO HANDLE MAIN UI TASKS
        Thread threadUI = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Infinite loop
                    while (true) {
                        // Only go here when all Wifi Direct nodes are established
                        if (wifiDirectConnected_all) {

                            // Standby loop
                            while (!isConnected_all) {
                                if (isConnected_SBC) {
                                    isConnected_all = true;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            statusBar.setBackgroundColor(Color.parseColor("#2196F3")); // Blue background
                                            statusBar.setText("Network established successfully!");
                                            buttonSTSP.setEnabled(true);
                                        }
                                    });
                                }
                                Thread.sleep(100);
                            }

                            waitFlag = true;
                            while (startRecording) {
                                if (waitFlag) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            statusBar.setBackgroundColor(Color.parseColor("#F44336")); // Red background
                                            statusBar.setText("Initializing sensors. Please stand still...");
                                            buttonSTSP.setEnabled(false);
                                        }
                                    });
                                    Thread.sleep(6000); // Wait 6 seconds for sensor calibration
                                    waitFlag = false;
                                }

                                if (isRecording_SBC) {
                                    isRecording_all = true;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            statusBar.setBackgroundColor(Color.parseColor("#8BC34A")); // Green background
                                            statusBar.setText("Data collection in progress");
                                            buttonSTSP.setEnabled(true);
//                                            button_Audio.setEnabled(true);

                                        }
                                    });
                                }
                                Thread.sleep(100);
                            }

                            if (isRecording_all) {
                                while (!startRecording) {
                                    if (waitFlag) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                statusBar.setBackgroundColor(Color.parseColor("#FFC107")); // Yellow background
                                                statusBar.setText("Stopping devices...");
                                                buttonSTSP.setEnabled(false);
//                                                button_Audio.setEnabled(false);
                                            }
                                        });
                                        Thread.sleep(5000); // Wait 5 seconds for stopping
                                        waitFlag = false;
                                    }

                                    if (!isRecording_SBC) {
                                        isRecording_all = false;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                statusBar.setBackgroundColor(Color.parseColor("#2196F3")); // Blue background
                                                statusBar.setText("System initiated!");
                                                buttonSTSP.setEnabled(true);

                                            }
                                        });
                                    }
                                    Thread.sleep(100);
                                }
                            }
                            Thread.sleep(1000);
                        }
                        // Go here when the nodes are not connected
                        else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
//                                    button_Audio.setEnabled(false);
                                }
                            });
                            while (!wifiDirectConnected_SBC) {
                                if (isFound_SBC) {
                                    Thread.sleep(500);
                                    WifiP2pConfig config = new WifiP2pConfig();
                                    config.wps.setup = WpsInfo.PBC;
                                    config.deviceAddress = deviceMACAddress_SBC;
                                    config.groupOwnerIntent = 15;
                                    if (ActivityCompat.checkSelfPermission(MoticonActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(MoticonActivity.this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                                        return;
                                    }
                                    manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                                        @Override
                                        public void onSuccess() {
                                            wifiDirectConnected_SBC = true;
                                            SBCStatusText.setText("Connecting");
                                            Log.d("WifiDirect", "Successfully sent connection request to SBC device!");
                                        }

                                        @Override
                                        public void onFailure(int reason) {
                                            Log.e("WifiDirect", "Unsuccessful connection attempt to the SBC device!-reason: " + reason);
                                            if (reason == WifiP2pManager.BUSY) {
                                                Log.e("WifiDirect", "Framework busy, retrying connection.");
                                            }
                                        }
                                    });
                                }
//                                Thread.sleep(1000);
                            }
                            Thread.sleep(500);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("Feedback","Command thread terminated!");
                }
            }
        });
        threadUI.start();

        // FUNCTION OF START/STOP BUTTON
        buttonSTSP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("MainUI","STSP button clicked!");
                // Global command
                if (!startRecording) {
                    AudioStart = true;

                    accEnableTime = SystemClock.elapsedRealtimeNanos() / 1000L -sessionStartTime;
                    // Get current date time to create file name for local file
//                    SimpleDateFormat formattedDateTime = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
//                    String currentDateTime = formattedDateTime.format(new Date());
//                    dataFileName = currentDateTime + ".dat";

                    buttonSTSP.setText("STOP RECORDING");
                    buttonSTSP.setEnabled(false);
                    sessionTime = System.currentTimeMillis() / 1000L; // Session time (s) un UNIX format for file name when the Start/Stop button is clicked
                    startRecording = true;

                    Log.d("Data Logger","Data file created!!!");

                    dataFileName = sessionTime + "_phone.dat";
                    dataFile = new File(getExternalFilesDir(null),"/LogFiles/" + dataFileName);
                    File dataDirectory = new File(getExternalFilesDir(null),"/LogFiles/");
                    if(!dataDirectory.exists()) {
                        dataDirectory.mkdirs();
                    }
                    if(!dataFile.exists()) {
                        try {
                            dataFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    MoticonFileName = sessionTime + "_Moticon.dat";
                    MoticonFile = new File(getExternalFilesDir(null),"/LogFiles/" + MoticonFileName);
                    File MoticonDirectory = new File(getExternalFilesDir(null),"/LogFiles/");
                    if(!MoticonDirectory.exists()) {
                        MoticonDirectory.mkdirs();
                    }
                    if(!MoticonFile.exists()) {
                        try {
                            MoticonFile.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
                else {
                    buttonSTSP.setText("START RECORDING");
                    buttonSTSP.setEnabled(false);
                    startRecording = false;
                    AudioStart = false;
                }

            }
        });


    }

    private void initiatePermission(){

        //PowerManagement permission
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                Log.i("Info","disable battery optimization...");

                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

        //BLE permission
        boolean bluetoothAvailable = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE);
        if (!bluetoothAvailable) {
            Log.i("Bluetooth", "Bluetooth is not supported");
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Log.i("Info","Bluetooth not enabled");
            // Device doesn't support Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_BLUETOOTH);
                Log.i("Info", "BLUETOOTH permission deny...");
                return;
            } else {
                Log.i("info", "BLUETOOTH permission granted");
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//            startActivityIntent.launch(enableBtIntent);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MY_PERMISSIONS_REQUEST_CONNECT);
            Log.i("Info", "BLUETOOTH CONNECTION permission deny...");
            return;
        } else {
            Log.i("info", "BLUETOOTH CONNECTION permission granted");
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, MY_PERMISSIONS_REQUEST_SCAN);
            Log.i("Info", "BLUETOOTH SCAN permission deny...");
            return;
        } else {
            Log.i("info", "BLUETOOTH SCAN permission granted");
        }




        //LocationPermission
        locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "First enable FINE LOCATION ACCESS in settings.", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            return;
        } else {
            Log.i("Info", "FINE LOCATION permission granted");
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "First enable COARSE LOCATION ACCESS in settings.", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
            return;
        } else {
            Log.i("Info", "COARSE LOCATION permission granted");
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "First enable BACKGROUND LOCATION ACCESS in settings.", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION);
            return;
        } else {
            Log.i("Info", "BACKGROUND LOCATION permission granted");
        }

        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.NEARBY_WIFI_DEVICES ) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.NEARBY_WIFI_DEVICES}, MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES);
            return;
        }else{
            Log.i("Info","NEARBY WIFI DEVICES permission granted");
        }
    }


    public void scanAndConnection() {
            Service.StartInsoleScan startInsoleScanMessage = Service.StartInsoleScan.newBuilder().build();
            Service.MoticonMessage moticonMessage1 = Service.MoticonMessage.newBuilder()
                    .setStartInsoleScan(startInsoleScanMessage)
                    .build();
            sendProtoToService(moticonMessage1.toByteArray());
            Log.i("Info","ble thread start");


            CompletableFuture.supplyAsync(() -> {
            // Process the message
            while (InsoleDeviceList.size() < 2) {
                ;
            }
            Log.i("Info","ble thread start");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoticonStatusText.setText("2 FOUND");
                }
            });


            Service.StopInsoleScan stopInsoleScanMessage = Service.StopInsoleScan.newBuilder().build();
            Service.MoticonMessage moticonMessage4 = Service.MoticonMessage.newBuilder()
                    .setStopInsoleScan(stopInsoleScanMessage)
                    .build();

            sendProtoToService(moticonMessage4.toByteArray());

            Log.i("Info", "InsoleDeviceList:  " + InsoleDeviceList);

            Service.ConnectInsoles connectInsolesMessage1 = Service.ConnectInsoles.newBuilder()
                    .addAllInsoles(InsoleDeviceList)
                    .build();
            Service.MoticonMessage moticonMessage3 = Service.MoticonMessage.newBuilder()
                    .setConnectInsoles(connectInsolesMessage1)
                    .build();

            sendProtoToService(moticonMessage3.toByteArray());

            while (sensorLeft == 0 || sensorRight == 0) {
                ;
            }
            runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MoticonStatusText.setText("2 connected");
                    }
            });

            try {
                Thread.sleep(1000);
            } catch (
                    InterruptedException e) {
                e.printStackTrace();
            }

            initiateSensor(insoleSerialNumbers);

            while (ZeroingComplete == false) {
                ;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MoticonStatusText.setText("Zeroing Complete");
                    progressBar_Moticon.setVisibility(View.INVISIBLE);
                    greencheck_Moticon.setVisibility(View.VISIBLE);
                    drawCheck(greencheck_Moticon);
                }
            });


            return 0;
            });


        }





    public void initiateSensor(List<Integer> SerialNumberList){
        Log.i("Info","initating sensors...");
        MoticonStartTime = SystemClock.elapsedRealtimeNanos()/1000L/1000L;
        //initiate sensors
        Common.ServiceConfig initServiceCfg = Common.ServiceConfig.newBuilder()
                .setServiceStartTime(MoticonStartTime)
                .setServiceId(Common.ServiceId.newBuilder().setLeftSerialNumber(SerialNumberList.get(0)).setRightSerialNumber(SerialNumberList.get(1)).build())
                .setServiceType(Common.ServiceType.LIVE)
                .setRate(100)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledPressure(true)
                .addEnabledAcceleration(true)
                .addEnabledAcceleration(true)
                .addEnabledAcceleration(true)
                .setAccGRange(Common.ServiceConfig.AccGRange.ACC_16_G)
                .setAccOdr(Common.ServiceConfig.AccOdr.ACC_208_ODR)
                .addEnabledAngular(true)
                .addEnabledAngular(true)
                .addEnabledAngular(true)
                .setAngDpsRange(Common.ServiceConfig.AngDpsRange.ANG_2000_DPS)
                .setAngOdr(Common.ServiceConfig.AngOdr.ANG_208_ODR)
                .setEnabledTemperature(true)
                .setEnabledTotalForce(true)
                .addEnabledCop(true)
                .addEnabledCop(true)
                .setActivityProfile(Common.ServiceConfig.ActivityProfile.ACTIVITY_PROFILE_CONTINUOUS)
                .setIsPreview(false)
                .build();

        Common.MeasurementInfo measurementInfo = Common.MeasurementInfo.newBuilder()
                .setName("")
                .setComment("")
                .build();

//        Log.i("Info","MoticonMessage: " + initServiceCfg);
        Common.ServiceEndpoint endpointCfg = Common.ServiceEndpoint.newBuilder()
                .setEndpointType(Common.ServiceEndpoint.EndpointType.APP)
                .build();

        Common.Zeroing leftZeroingCfg = Common.Zeroing.newBuilder()
                .setSource(Common.ZeroingSource.KEEP)
                .setMode(Common.ZeroingMode.AUTO)
                .setSide(Common.Side.LEFT)
                .build();

        Common.Zeroing rightZeroingCfg = Common.Zeroing.newBuilder()
                .setSource(Common.ZeroingSource.KEEP)
                .setMode(Common.ZeroingMode.AUTO)
                .setSide(Common.Side.RIGHT)
                .build();

        // set sensor information
        Service.StartService startServiceMessage = Service.StartService.newBuilder()
                .setServiceConfig(initServiceCfg)
                .setMeasurementInfo(measurementInfo)
                .setServiceEndpoint(endpointCfg)
                .build();

        Service.MoticonMessage leftZeroingMessage = Service.MoticonMessage.newBuilder()
                .setZeroing(leftZeroingCfg)
                .build();
        sendProtoToService(leftZeroingMessage.toByteArray());

        Service.MoticonMessage rightZeroingMessage = Service.MoticonMessage.newBuilder()
                .setZeroing(rightZeroingCfg)
                .build();
        sendProtoToService(rightZeroingMessage.toByteArray());

        Service.MoticonMessage moticonMessage5 = Service.MoticonMessage.newBuilder()
                .setStartService(startServiceMessage)
                .build();
        sendProtoToService(moticonMessage5.toByteArray());
        Log.i("Info: ", "startServiceMessage: " + moticonMessage5);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                BLE_status = true;
                Log.i("BLE_status: ", "enabled");
                // Bluetooth is now enabled
            } else {
                // User declined to enable Bluetooth
                BLE_status = false;
                Log.i("BLE_status: ", "disabled");
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(br);
        unbindService();

        handlerThread_P2P.quitSafely();
        unregisterReceiver(receiver);

        mSensorManager.unregisterListener(sensorEventListener);
        handlerThread_Sensor.quitSafely();

        executorRecv_WritePhone.shutdownNow();
        executorRecv_WriteMoticon.shutdownNow();

        executorRecv_WriteAudio.shutdownNow();
        executorRecv_WriteAudioTime.shutdownNow();




    }



    private boolean bindService(Context context, boolean standalone) {
        Intent mIntentInsole3 = new Intent(this, Insole3Service.class);
        mIntentInsole3.putExtra("standalone", standalone);

        Log.i(TAG, "Start the service");
        try {
            if (mBound) {
                Log.w(TAG, "Service is already bound. Do not bind again");
            } else {
                context.startService(mIntentInsole3);
                Log.i(TAG, "Bind to the service");
                context.bindService(mIntentInsole3, mServiceConnection, 0);
                mBound = true;
            }
        } catch (IllegalStateException illegalStateException) {
            Log.e(TAG, "Can't bind service as it seems to be in the background: " +
                    illegalStateException);
        }
        return mBound;
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("Info", "onServiceConnected");
            Log.i("Info","Service type: " + service.getClass().getName());
            mInsole3Service = (Insole3Service.Insole3Binder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i("Info", "onServiceDisconnected");
            mInsole3Service = null;
        }
    };

    private void unbindService() {
        Log.i("Info", "Unbind from the service ");
        mBound = false;
        try {
            unbindService(mServiceConnection);
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.e("Info", "Can't unbind service as it seems not bound: " +
                    illegalArgumentException);
        }
    }

    private void sendProtoToService(byte[] message) {
        try{
            Intent broadcast = new Intent(Insole3Service.BROADCAST_CONTROLLER_MSG);
            broadcast.putExtra(Insole3Service.EXTRA_PROTO_MSG, message);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcast);
//        sendBroadcast(broadcast);
        }catch(Exception e)
        {
            Log.e("sendProtoToService","Failed to send proto message",e);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Info", "FINE permission granted");
                // Permission was granted; you can now use location services
            } else {
                // Permission was denied; handle it accordingly
                Toast.makeText(getApplicationContext(), "FINE Location permission denied. You may not be able to use location-related features.", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == MY_PERMISSIONS_REQUEST_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Info", "COARSE permission granted");
                // Permission was granted; you can now use location services
            } else {
                // Permission was denied; handle it accordingly
                Toast.makeText(getApplicationContext(), "COARSE Location permission denied. You may not be able to use location-related features.", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Info", "BACKGROUND permission granted");
                // Permission was granted; you can now use location services
            } else {
                // Permission was denied; handle it accordingly
                Toast.makeText(getApplicationContext(), "BACKGROUND Location permission denied. You may not be able to use location-related features.", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == MY_PERMISSIONS_REQUEST_BLUETOOTH) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Info", "Bluetooth permission granted");
                // Permission was granted; you can now use location services
            } else {
                // Permission was denied; handle it accordingly
                Toast.makeText(getApplicationContext(), "Bluetooth permission denied. You may not be able to use bluetooth-related features.", Toast.LENGTH_LONG).show();
            }

        }

        if (requestCode == MY_PERMISSIONS_REQUEST_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Info", "Connection permission granted");
                // Permission was granted; you can now use location services
            } else {
                // Permission was denied; handle it accordingly
                Toast.makeText(getApplicationContext(), "Bluetooth connection denied. You may not be able to use connection-related features.", Toast.LENGTH_LONG).show();
            }

        }

        if (requestCode == MY_PERMISSIONS_REQUEST_SCAN) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Info", "SCAN permission granted");
                // Permission was granted; you can now use location services
            } else {
                // Permission was denied; handle it accordingly
                Toast.makeText(getApplicationContext(), "Bluetooth connection denied. You may not be able to use connection-related features.", Toast.LENGTH_LONG).show();
            }

        }

        if (requestCode == MY_PERMISSIONS_REQUEST_NEARBY_WIFI_DEVICES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Info", "NEARBY WIFI DEVICES permission granted");
                // Permission was granted; you can now use location services
            } else {
                // Permission was denied; handle it accordingly
                Toast.makeText(getApplicationContext(), "Bluetooth connection denied. You may not be able to use connection-related features.", Toast.LENGTH_LONG).show();
            }

        }


    }

    private BroadcastReceiver createServiceMsgReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                Toast.makeText(getApplicationContext(), "broadcast receiver start...", Toast.LENGTH_SHORT).show();
//                Log.i("Info","broadcast receiver start...");
                String action = intent.getAction();
                Log.i("Info","Action: " + action);
                byte[] protoMsg = intent.getByteArrayExtra(Insole3Service.EXTRA_PROTO_MSG);
                try {Service.MoticonMessage moticonMessage = Service.MoticonMessage.parseFrom(protoMsg);
                    Log.i("Info","MoticonMessageCase: " + moticonMessage.getMsgCase());

                    // Continue processing the received message
                    switch(moticonMessage.getMsgCase()){
                        case ZEROING_CONF:
                            Log.i("Info","zeroing: "+ moticonMessage.getZeroingConf().getSide() + " "+ moticonMessage.getZeroingConf().getError());
                            if (moticonMessage.getZeroingConf().getError().getErrorCode() == Common.ErrorCode.SUCCESS) {
                                if (moticonMessage.getZeroingConf().getSide() == Common.Side.LEFT) {
                                    leftZeroSuccess = 1;
                                } else {
                                    rightZeroSuccess = 1;
                                }
                            }

                            if (leftZeroSuccess+rightZeroSuccess==2){
                                ZeroingComplete = true;
                            }
                            break;

                        case TIMESTAMP_CONF:
                            Log.i("Info","TIMESTAMP!!");
                            Long Tx = moticonMessage.getTimestampConf().getTimeTx();
                            Long Rx = moticonMessage.getTimestampConf().getTimeRx();
                            Log.i("Info","Tx: " + Tx + " " + "Rx: "+ Rx);
                            break;
                        case DATA_MESSAGE:
                            // HANDLE PROTO MESSAGES HERE:
                                MoticonReceivedTime = SystemClock.elapsedRealtimeNanos() / 1000L -sessionStartTime;
                                Long sensorTime = moticonMessage.getDataMessage().getTime();
                                MoticonTime = sensorTime+MoticonServiceStartTime-sessionStartTime/1000L;

                                Log.i("Info","receive Time: " + MoticonReceivedTime + "" + "ServiceStartTime: "+ MoticonServiceStartTime +"MoticonTime: " + MoticonTime);
                                String sensorSide = moticonMessage.getDataMessage().getSide().name();
//                                int sensorSide = moticonMessage.getDataMessage().getSide().getNumber(
                                List<Float> acc = moticonMessage.getDataMessage().getAccelerationList();
                                List<Float> angularVal = moticonMessage.getDataMessage().getAngularList();
                                List<Integer> pressureVal = moticonMessage.getDataMessage().getPressureList();
                                int totalForceVal = moticonMessage.getDataMessage().getTotalForce();
                                float temperatureVal = moticonMessage.getDataMessage().getTemperature();
                                List<Float> copVal = moticonMessage.getDataMessage().getCopList();

                                if (sensorSide.equals("LEFT")) {
                                    Log.i("Info","Left found");
//                                    MoticonTime = sensorTime+MoticonServiceS
                                    MoticonACC_Left[0] = acc.get(0);
                                    MoticonACC_Left[1] = acc.get(1);
                                    MoticonACC_Left[2] = acc.get(2);
                                    MoticonAngular_Left[0] = angularVal.get(0);
                                    MoticonAngular_Left[1] = angularVal.get(1);
                                    MoticonAngular_Left[2] = angularVal.get(2);
                                    MoticonPressure_Left[0] = pressureVal.get(0);
                                    MoticonPressure_Left[1] = pressureVal.get(1);
                                    MoticonPressure_Left[2] = pressureVal.get(2);
                                    MoticonPressure_Left[3] = pressureVal.get(3);
                                    MoticonPressure_Left[4] = pressureVal.get(4);
                                    MoticonPressure_Left[5] = pressureVal.get(5);
                                    MoticonPressure_Left[6] = pressureVal.get(6);
                                    MoticonPressure_Left[7] = pressureVal.get(7);
                                    MoticonPressure_Left[8] = pressureVal.get(8);
                                    MoticonPressure_Left[9] = pressureVal.get(9);
                                    MoticonPressure_Left[10] = pressureVal.get(10);
                                    MoticonPressure_Left[11] = pressureVal.get(11);
                                    MoticonPressure_Left[12] = pressureVal.get(12);
                                    MoticonPressure_Left[13] = pressureVal.get(13);
                                    MoticonPressure_Left[14] = pressureVal.get(14);
                                    MoticonPressure_Left[15] = pressureVal.get(15);
                                    MoticonTemp_Left = temperatureVal;
                                    MoticonTotalForce_Left = totalForceVal;
                                    MoticonCOP_Left[0] = copVal.get(0);
                                    MoticonCOP_Left[1] = copVal.get(1);
                                    DataSide = 0;

                                }else if (sensorSide.equals("RIGHT")){
                                    MoticonACC_Right[0] = acc.get(0);
                                    MoticonACC_Right[1] = acc.get(1);
                                    MoticonACC_Right[2] = acc.get(2);
                                    MoticonAngular_Right[0] = angularVal.get(0);
                                    MoticonAngular_Right[1] = angularVal.get(1);
                                    MoticonAngular_Right[2] = angularVal.get(2);
                                    MoticonPressure_Right[0] = pressureVal.get(0);
                                    MoticonPressure_Right[1] = pressureVal.get(1);
                                    MoticonPressure_Right[2] = pressureVal.get(2);
                                    MoticonPressure_Right[3] = pressureVal.get(3);
                                    MoticonPressure_Right[4] = pressureVal.get(4);
                                    MoticonPressure_Right[5] = pressureVal.get(5);
                                    MoticonPressure_Right[6] = pressureVal.get(6);
                                    MoticonPressure_Right[7] = pressureVal.get(7);
                                    MoticonPressure_Right[8] = pressureVal.get(8);
                                    MoticonPressure_Right[9] = pressureVal.get(9);
                                    MoticonPressure_Right[10] = pressureVal.get(10);
                                    MoticonPressure_Right[11] = pressureVal.get(11);
                                    MoticonPressure_Right[12] = pressureVal.get(12);
                                    MoticonPressure_Right[13] = pressureVal.get(13);
                                    MoticonPressure_Right[14] = pressureVal.get(14);
                                    MoticonPressure_Right[15] = pressureVal.get(15);
                                    MoticonTemp_Right = temperatureVal;
                                    MoticonTotalForce_Right = totalForceVal;
                                    MoticonCOP_Right[0] = copVal.get(0);
                                    MoticonCOP_Right[1] = copVal.get(1);
                                    DataSide = 1;
                                }
                                sensorReading_Moticon.setText(Float.toString(acc.get(2)));

                                if (isRecording_all) {
                                    LogPacket_Moticon = createLogPacket_Moticon(MoticonTime, MoticonReceivedTime,
                                            MoticonACC_Left, MoticonAngular_Left, MoticonPressure_Left, MoticonTemp_Left, MoticonTotalForce_Left, MoticonCOP_Left,
                                            MoticonACC_Right, MoticonAngular_Right, MoticonPressure_Right, MoticonTemp_Right, MoticonTotalForce_Right, MoticonCOP_Right,DataSide);
                                    PACKET_NUMBER_Moticon = PACKET_NUMBER_Moticon + 1;
                                    if (CURR_PACKET_MOTICON==1){
                                        addPackets_Moticon(PACKET_NUMBER_Moticon,COMBINED_PACKET_MOTICON1,LogPacket_Moticon);
//                                        buffer_moticon1.put(LogPacket_Moticon);
                                    }else if (CURR_PACKET_MOTICON==2){
                                        addPackets_Moticon(PACKET_NUMBER_Moticon,COMBINED_PACKET_MOTICON2,LogPacket_Moticon);
//                                        buffer_moticon2.put(LogPacket_Moticon);
                                    }
                                    if (PACKET_NUMBER_Moticon == 9118) {
//                                        COMBINED_PACKET_MOTICON = buffer_moticon.array();
//                                        try (FileOutputStream fileOut_Moticon = new FileOutputStream(MoticonFile, true)) {
//                                            fileOut_Moticon.write(COMBINED_PACKET_MOTICON);
//                                        } catch (IOException e) {
//                                            e.printStackTrace();
//                                        }
//                                        buffer_moticon.clear();
//                                        Arrays.fill(COMBINED_PACKET_MOTICON, (byte) 0);

                                        if (CURR_PACKET_MOTICON==1){
//                                            COMBINED_PACKET_MOTICON1 = buffer_moticon1.array();
                                            PACKET_MOTICON1_STATUS = true;
                                            CURR_PACKET_MOTICON=2;
//                                            buffer_moticon1.clear();
                                        }else if (CURR_PACKET_MOTICON==2){
//                                            COMBINED_PACKET_MOTICON2 = buffer_moticon2.array();
                                            PACKET_MOTICON2_STATUS = true;
                                            CURR_PACKET_MOTICON=1;
//                                            buffer_moticon2.clear();
                                        }

                                        executorRecv_WriteMoticon.submit(new Runnable(){
                                            @Override
                                            public void run(){
                                                writeMoticonDataToFile();
                                            }
                                        });

                                        PACKET_NUMBER_Moticon = 0;
                                    }
                                }else if(!isRecording_all && (COMBINED_PACKET_MOTICON1[0]==1 ||COMBINED_PACKET_MOTICON2[0]==1)) {
                                    Log.i("Info","writing last moticon buffer");
                                    executorRecv_WritePhone.submit(new Runnable(){
                                        @Override
                                        public void run(){
                                            writeMoticonDataToFile();
                                        }
                                    });
                                }
                            break;

                        case INSOLE_ADVERTISEMENT:
                            Log.i("Info","Received advertisement data");
                            Log.i("Info","device found: "+ InsoleDeviceList.size());
                            Service.InsoleDevice insoleDevice = moticonMessage.getInsoleAdvertisement().getInsole();
                            String insoleAddress = insoleDevice.getDeviceAddress();
                            Common.Side insoleSide = insoleDevice.getSide();
                            int batteryLevel = moticonMessage.getInsoleAdvertisement().getBatteryLevel();
                            int insoleSize = insoleDevice.getSize();
//                            Log.i("Info", "Sensor Found: " + insoleAddress + "side " + insoleSide + "size: " + insoleSize + "battery: " + batteryLevel);
                            if (InsoleDeviceList.size()<2){
                                if (!InsoleAddressList.contains(insoleAddress)){
                                    InsoleAddressList.add(insoleAddress);
                                    InsoleDeviceList.add(insoleDevice);
                                    if (insoleDevice.getSide().name().equals("LEFT")){
                                        insoleSerialNumbers.set(0,insoleDevice.getSerialNumber());
                                        Log.i("Info","Left found");}
                                    else{
                                        insoleSerialNumbers.set(1,insoleDevice.getSerialNumber());
                                        Log.i("Info","Right found");}



//                                    Log.i("Info","InsoleDeviceList: "+ insoleDevice);
                                }
                            }
                            break;
                        case INSOLE_CONNECTION_STATUS:
                             Service.InsoleConnectionStatus.Status sensorConnect = moticonMessage.getInsoleConnectionStatus().getStatus();
                             if (sensorConnect == Service.InsoleConnectionStatus.Status.READY){
                                 if (moticonMessage.getInsoleConnectionStatus().getSide().name().equals("LEFT")) {
                                     sensorLeft = 1;
                                 } else if (moticonMessage.getInsoleConnectionStatus().getSide().name().equals("RIGHT")) {
                                     sensorRight =1;
                                 }
                             }
                             Log.i("Info","connectedSensor:  Left"+ sensorLeft + " Right: " + sensorRight);
                            break;
                        case START_SERVICE_CONF:
                            if (moticonMessage.getStartServiceConf().getLeftStartServiceConf().getError().getErrorCode() == Common.ErrorCode.SUCCESS) {
                                leftConfigSuccess = 1;

                            }
                            if (moticonMessage.getStartServiceConf().getRightStartServiceConf().getError().getErrorCode() == Common.ErrorCode.SUCCESS) {
                                rightConfigSuccess = 1;
                            }
                            Log.i("Info","config  Left"+ leftConfigSuccess + " config Right: " + rightConfigSuccess);
                            MoticonServiceStartTime = moticonMessage.getStartServiceConf().getStatusInfo().getInsoleStatusInfo(0).getServiceConfigs(0).getServiceStartTime();
                            break;


                        default:
                            Log.w("Info", "Unhandled MsgCase = " + moticonMessage.getMsgCase());
                            break;
                    }
                } catch (InvalidProtocolBufferException e) {
                    // Handle the exception
                    Log.i("Info","Invalid Protocol Buffer");
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
//                Toast.makeText(getApplicationContext(), "Scan successful!",Toast.LENGTH_SHORT).show();
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];

                int index = 0;
                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    Log.i("WifiDirect", device.deviceName);
                    index++;

                    if (device.deviceName.equals(deviceName_SBC)) {
                        deviceMACAddress_SBC = device.deviceAddress;
                        isFound_SBC = true;
                        Log.i("WifiDirect", "Found SBC!");
                        updateWifiDirectStatus("SBC", isFound_SBC, wifiDirectConnected_SBC);
                    }

//                    else if (device.deviceName.equals(deviceName_R)) {
//                        deviceMACAddress_R = device.deviceAddress;
//                        isFound_R = true;
//                        updateWifiDirectStatus("R", isFound_R,wifiDirectConnected_R);
//                    }
                }
            }
            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No device found!", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            if (info.groupFormed && info.isGroupOwner) {
                InetAddress groupOwnerAdd = info.groupOwnerAddress;
                String ownerIP = groupOwnerAdd.getHostAddress();
                Log.i("GroupOwnerIP",ownerIP);
            }
        }
    };

    WifiP2pManager.GroupInfoListener groupInfoListener = new WifiP2pManager.GroupInfoListener() {
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {

            Collection clientList = group.getClientList();

            for (Iterator iter = clientList.iterator(); iter.hasNext();) {
                WifiP2pDevice clientInfo = (WifiP2pDevice) iter.next();
                String clientAdd = clientInfo.deviceAddress;
                String clientName = clientInfo.deviceName;
                Log.i("ClientName", clientName);
                Log.i("ClientAdd", clientAdd);
                if (clientName.equals(deviceName_SBC)) {
                    if (!clientObjectCreated_SBC) {
                        wifiDirectConnected_SBC = true;
                        updateWifiDirectStatus("SBC", isFound_SBC, wifiDirectConnected_SBC);
                        clientObject_SBC = new ClientCommunication(ipAddress_SBC, port_SBC, "SBC");
                        clientObject_SBC.start();
                        clientObjectCreated_SBC = true;
                    }
                }

            }
            if (wifiDirectConnected_SBC) {
                wifiDirectConnected_all = true;
            }
        }
    };

    public void removeExistingGroup() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.requestGroupInfo(channel, group -> {
            if (group != null) {
                Log.d("WifiDirect", "Previous group found!");
                removePersistentGroups();
                manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("WifiDirect", "Group removal successful!");
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e("WifiDirect", "Group removal failed with error " + reason);
                    }
                });
            } else {

            }
        });
    }

    private void removePersistentGroups() {
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int i = 0; i < methods.length; i++) {
                if (methods[i].getName().equals("deletePersistentGroup")) {
                    // Remove any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[i].invoke(manager, channel,netid, null);
                    }
                }
            }
            Log.i("WifiDirect", "Persistent groups removed");
        } catch(Exception e) {
            Log.e("WifiDirect", "Failure removing persistent groups: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // UPDATE UI BASED ON THE CONNECTION STATUS OF THE DEVICES
    public void updateWifiDirectStatus(String dev, boolean isFound, boolean isConnected) {
        switch (dev) {
            case "SBC":
                if (isFound) {
                    if (isConnected) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SBCStatusText.setText("Connected");
//                                progressBar_SBC.setVisibility(View.INVISIBLE);
//                                greencheck_SBC.setVisibility(View.VISIBLE);
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SBCStatusText.setText("Found");
                            }
                        });
                    }
                }
                break;

        }
    }

    public byte[] createLogPacket_AudioTime(long audioStartTimeRec, int dataSize){
        byte[] buffer = new byte[18];

        byte[] header = new byte[3];
        header[0] = 1;
        header[1] = 2;
        header[2] = 3;

        byte[] tail = new byte[3];
        tail[0] = 4;
        tail[1] = 5;
        tail[2] = 6;

        byte[] audioStartTimeRecBytes = longToBytes(audioStartTimeRec);
        byte[] audioDataSizeBytes = intToBytes(dataSize);
        System.arraycopy(header, 0, buffer, 0, 3);
        System.arraycopy(audioStartTimeRecBytes, 0, buffer, 3, 8);
        System.arraycopy(audioDataSizeBytes, 0, buffer, 11, 4);
        System.arraycopy(tail, 0, buffer, 15, 3);

        return buffer;
    }

    public byte[] createLogPacket_AudioTime_rev002(long audioStartTimeRec, long audioFrame){
        byte[] buffer = new byte[22];

        byte[] header = new byte[3];
        header[0] = 1;
        header[1] = 2;
        header[2] = 3;

        byte[] tail = new byte[3];
        tail[0] = 4;
        tail[1] = 5;
        tail[2] = 6;

        byte[] audioStartTimeRecBytes = longToBytes(audioStartTimeRec);
        byte[] audioDataSizeBytes = longToBytes(audioFrame);
        System.arraycopy(header, 0, buffer, 0, 3);
        System.arraycopy(audioStartTimeRecBytes, 0, buffer, 3, 8);
        System.arraycopy(audioDataSizeBytes, 0, buffer, 11, 8);
        System.arraycopy(tail, 0, buffer, 19, 3);

        return buffer;
    }

    // FUNCTION TO CREATE LOG PACKET
    public byte[] createLogPacket(float[] sensorVals, long currentTime, long T1, long T2, long T3, long T4) {
        byte[] buffer = new byte[LOG_PACKET_LENGTH];

        byte[] header = new byte[3];
        header[0] = 1;
        header[1] = 2;
        header[2] = 3;

        byte[] tail = new byte[3];
        tail[0] = 4;
        tail[1] = 5;
        tail[2] = 6;

        byte[] currentTimeBytes = longToBytes(currentTime);

        byte[] AccXBytes_Phone = floatToBytes(sensorVals[0]);
        byte[] AccYBytes_Phone = floatToBytes(sensorVals[1]);
        byte[] AccZBytes_Phone = floatToBytes(sensorVals[2]);

        byte[] T1Bytes = longToBytes(T1);
        byte[] T2Bytes = longToBytes(T2);
        byte[] T3Bytes = longToBytes(T3);
        byte[] T4Bytes = longToBytes(T4);

        System.arraycopy(header, 0, buffer, 0, 3);
        System.arraycopy(currentTimeBytes, 0, buffer, 3, 8);
        System.arraycopy(AccXBytes_Phone, 0, buffer, 11, 4);
        System.arraycopy(AccYBytes_Phone, 0, buffer, 15, 4);
        System.arraycopy(AccZBytes_Phone, 0, buffer, 19, 4);
        System.arraycopy(T1Bytes, 0, buffer, 23, 8);
        System.arraycopy(T2Bytes, 0, buffer, 31, 8);
        System.arraycopy(T3Bytes, 0, buffer, 39, 8);
        System.arraycopy(T4Bytes, 0, buffer, 47, 8);
        System.arraycopy(tail, 0, buffer, 55, 3);

        return buffer;
    }

    public byte[] createLogPacket_Moticon(long MoticonTime, long MoticonReceivedTime,
                                          float[] MoticonACC_Left, float[] MoticonAngular_Left, int[] MoticonPressure_Left, float MoticonTemp_Left, int MoticonTotalForce_Left, float[] MoticonCOP_Left,
                                          float[] MoticonACC_Right,  float[] MoticonAngular_Right, int[] MoticonPressure_Right, float MoticonTemp_Right, int MoticonTotalForce_Right, float[] MoticonCOP_Right, int DataSide) {
        byte[] buffer2 = new byte[LOG_PACKET_LENGTH_MOTICON];

        byte[] header = new byte[3];
        header[0] = 1;
        header[1] = 2;
        header[2] = 3;

        byte[] tail = new byte[3];
        tail[0] = 4;
        tail[1] = 5;
        tail[2] = 6;

        byte[] MoticonTimeBytes = longToBytes(MoticonTime);
        byte[] MoticonReceivedTimeBytes = longToBytes(MoticonReceivedTime);

        byte[] AccXBytes_Left = floatToBytes(MoticonACC_Left[0]);
        byte[] AccYBytes_Left = floatToBytes(MoticonACC_Left[1]);
        byte[] AccZBytes_Left = floatToBytes(MoticonACC_Left[2]);

        byte[] AngXBytes_Left = floatToBytes(MoticonAngular_Left[0]);
        byte[] AngYBytes_Left = floatToBytes(MoticonAngular_Left[1]);
        byte[] AngZBytes_Left = floatToBytes(MoticonAngular_Left[2]);

        byte[] P1Bytes_Left = intToBytes(MoticonPressure_Left[0]);
        byte[] P2Bytes_Left = intToBytes(MoticonPressure_Left[1]);
        byte[] P3Bytes_Left = intToBytes(MoticonPressure_Left[2]);
        byte[] P4Bytes_Left = intToBytes(MoticonPressure_Left[3]);
        byte[] P5Bytes_Left = intToBytes(MoticonPressure_Left[4]);
        byte[] P6Bytes_Left = intToBytes(MoticonPressure_Left[5]);
        byte[] P7Bytes_Left = intToBytes(MoticonPressure_Left[6]);
        byte[] P8Bytes_Left = intToBytes(MoticonPressure_Left[7]);
        byte[] P9Bytes_Left = intToBytes(MoticonPressure_Left[8]);
        byte[] P10Bytes_Left = intToBytes(MoticonPressure_Left[9]);
        byte[] P11Bytes_Left = intToBytes(MoticonPressure_Left[10]);
        byte[] P12Bytes_Left = intToBytes(MoticonPressure_Left[11]);
        byte[] P13Bytes_Left = intToBytes(MoticonPressure_Left[12]);
        byte[] P14Bytes_Left = intToBytes(MoticonPressure_Left[13]);
        byte[] P15Bytes_Left = intToBytes(MoticonPressure_Left[14]);
        byte[] P16Bytes_Left = intToBytes(MoticonPressure_Left[15]);

        byte[] TempBytes_Left = floatToBytes(MoticonTemp_Left);
        byte[] TotalForceBytes_Left = intToBytes(MoticonTotalForce_Left);
        byte[] COPX_Left = floatToBytes(MoticonCOP_Left[0]);
        byte[] COPY_Left = floatToBytes(MoticonCOP_Left[1]);

        byte[] AccXBytes_Right = floatToBytes(MoticonACC_Right[0]);
        byte[] AccYBytes_Right = floatToBytes(MoticonACC_Right[1]);
        byte[] AccZBytes_Right = floatToBytes(MoticonACC_Right[2]);

        byte[] AngXBytes_Right = floatToBytes(MoticonAngular_Right[0]);
        byte[] AngYBytes_Right = floatToBytes(MoticonAngular_Right[1]);
        byte[] AngZBytes_Right = floatToBytes(MoticonAngular_Right[2]);

        byte[] P1Bytes_Right = intToBytes(MoticonPressure_Right[0]);
        byte[] P2Bytes_Right = intToBytes(MoticonPressure_Right[1]);
        byte[] P3Bytes_Right = intToBytes(MoticonPressure_Right[2]);
        byte[] P4Bytes_Right = intToBytes(MoticonPressure_Right[3]);
        byte[] P5Bytes_Right = intToBytes(MoticonPressure_Right[4]);
        byte[] P6Bytes_Right = intToBytes(MoticonPressure_Right[5]);
        byte[] P7Bytes_Right = intToBytes(MoticonPressure_Right[6]);
        byte[] P8Bytes_Right = intToBytes(MoticonPressure_Right[7]);
        byte[] P9Bytes_Right = intToBytes(MoticonPressure_Right[8]);
        byte[] P10Bytes_Right = intToBytes(MoticonPressure_Right[9]);
        byte[] P11Bytes_Right = intToBytes(MoticonPressure_Right[10]);
        byte[] P12Bytes_Right = intToBytes(MoticonPressure_Right[11]);
        byte[] P13Bytes_Right = intToBytes(MoticonPressure_Right[12]);
        byte[] P14Bytes_Right = intToBytes(MoticonPressure_Right[13]);
        byte[] P15Bytes_Right = intToBytes(MoticonPressure_Right[14]);
        byte[] P16Bytes_Right = intToBytes(MoticonPressure_Right[15]);

        byte[] TempBytes_Right = floatToBytes(MoticonTemp_Right);
        byte[] TotalForceBytes_Right = intToBytes(MoticonTotalForce_Right);
        byte[] COPX_Right = floatToBytes(MoticonCOP_Right[0]);
        byte[] COPY_Right = floatToBytes(MoticonCOP_Right[1]);
        byte[] Bytes_DataSide = intToBytes(DataSide);

        System.arraycopy(header, 0, buffer2, 0, 3);
        System.arraycopy(MoticonTimeBytes, 0, buffer2, 3, 8);
        System.arraycopy(MoticonReceivedTimeBytes, 0, buffer2, 11, 8);
        System.arraycopy(AccXBytes_Left, 0, buffer2, 19, 4);
        System.arraycopy(AccYBytes_Left, 0, buffer2, 23, 4);
        System.arraycopy(AccZBytes_Left, 0, buffer2, 27, 4);
        System.arraycopy(AngXBytes_Left, 0, buffer2, 31, 4);
        System.arraycopy(AngYBytes_Left, 0, buffer2, 35, 4);
        System.arraycopy(AngZBytes_Left, 0, buffer2, 39, 4);
        System.arraycopy(P1Bytes_Left,0,buffer2,43,4);
        System.arraycopy(P2Bytes_Left,0,buffer2,47,4);
        System.arraycopy(P3Bytes_Left,0,buffer2,51,4);
        System.arraycopy(P4Bytes_Left,0,buffer2,55,4);
        System.arraycopy(P5Bytes_Left,0,buffer2,59,4);
        System.arraycopy(P6Bytes_Left,0,buffer2,63,4);
        System.arraycopy(P7Bytes_Left,0,buffer2,67,4);
        System.arraycopy(P8Bytes_Left,0,buffer2,71,4);
        System.arraycopy(P9Bytes_Left,0,buffer2,75,4);
        System.arraycopy(P10Bytes_Left,0,buffer2,79,4);
        System.arraycopy(P11Bytes_Left,0,buffer2,83,4);
        System.arraycopy(P12Bytes_Left,0,buffer2,87,4);
        System.arraycopy(P13Bytes_Left,0,buffer2,91,4);
        System.arraycopy(P14Bytes_Left,0,buffer2,95,4);
        System.arraycopy(P15Bytes_Left,0,buffer2,99,4);
        System.arraycopy(P16Bytes_Left,0,buffer2,103,4);
        System.arraycopy(TempBytes_Left,0,buffer2,107,4);
        System.arraycopy(TotalForceBytes_Left,0,buffer2,111,4);
        System.arraycopy(COPX_Left,0,buffer2,115,4);
        System.arraycopy(COPY_Left,0,buffer2,119,4);

        System.arraycopy(AccXBytes_Right, 0, buffer2, 123, 4);
        System.arraycopy(AccYBytes_Right, 0, buffer2, 127, 4);
        System.arraycopy(AccZBytes_Right, 0, buffer2, 131, 4);
        System.arraycopy(AngXBytes_Right, 0, buffer2, 135, 4);
        System.arraycopy(AngYBytes_Right, 0, buffer2, 139, 4);
        System.arraycopy(AngZBytes_Right, 0, buffer2, 143, 4);
        System.arraycopy(P1Bytes_Right,0,buffer2,147,4);
        System.arraycopy(P2Bytes_Right,0,buffer2,151,4);
        System.arraycopy(P3Bytes_Right,0,buffer2,155,4);
        System.arraycopy(P4Bytes_Right,0,buffer2,159,4);
        System.arraycopy(P5Bytes_Right,0,buffer2,163,4);
        System.arraycopy(P6Bytes_Right,0,buffer2,167,4);
        System.arraycopy(P7Bytes_Right,0,buffer2,171,4);
        System.arraycopy(P8Bytes_Right,0,buffer2,175,4);
        System.arraycopy(P9Bytes_Right,0,buffer2,179,4);
        System.arraycopy(P10Bytes_Right,0,buffer2,183,4);
        System.arraycopy(P11Bytes_Right,0,buffer2,187,4);
        System.arraycopy(P12Bytes_Right,0,buffer2,191,4);
        System.arraycopy(P13Bytes_Right,0,buffer2,195,4);
        System.arraycopy(P14Bytes_Right,0,buffer2,199,4);
        System.arraycopy(P15Bytes_Right,0,buffer2,203,4);
        System.arraycopy(P16Bytes_Right,0,buffer2,207,4);
        System.arraycopy(TempBytes_Right,0,buffer2,211,4);
        System.arraycopy(TotalForceBytes_Right,0,buffer2,215,4);
        System.arraycopy(COPX_Right,0,buffer2,219,4);
        System.arraycopy(COPY_Right,0,buffer2,223,4);
        System.arraycopy(Bytes_DataSide,0,buffer2,227,4);

        System.arraycopy(tail, 0, buffer2, 231, 3);

        return buffer2;
    }
    // CONVERT LONG TO BYTES
    public byte[] longToBytes(long longVal) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putLong(longVal);
        return buffer.array();
    }

    // CONVERT FLOAT TO BYTES
    public byte[] floatToBytes(float floatVal) {
        ByteBuffer buffer = ByteBuffer.allocate(Float.BYTES);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putFloat(floatVal);
        return buffer.array();
    }

    //CONVERT INTEGAR TO BYTES
    public byte[] intToBytes(int intVal){
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(intVal) ;
        return buffer.array();
    }
    public class ClientCommunication extends Thread {
        String ipAddress;
        int portNumber;
        String devLabel;
        int attemptCount = 0;
        int attemptLimit = 5;
        boolean isRecording = false;
        boolean isConnected = false;
        boolean isOff = false;
        long startTime;
        long sessionTimestamp = 0;
        long receiveTime = 0;
        long sendbackTime = 0;
        int packetCount = 0;

        public ClientCommunication(String ip, int port, String dev) {
            portNumber = port;
            ipAddress = ip;
            devLabel = dev;
        }

        // CONVERT LONG TO BYTES
        public byte[] longToBytes(long longVal) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putLong(longVal);
            return buffer.array();
        }

        // CONVERT BYTES TO LONG
        public long bytesToLong(byte[] byteArray) {
            // MSB AND LSB
            long longVal =
                    ((byteArray[0] & 0xFFL) << 24)      //00000000_00000000_00000000_00000000_11111111_00000000_00000000_00000000
                            | ((byteArray[1] & 0xFFL) << 16)    //00000000_00000000_00000000_00000000_00000000_11111111_00000000_00000000
                            | ((byteArray[2] & 0xFFL) << 8)   //00000000_00000000_00000000_00000000_00000000_00000000_11111111_00000000
                            | (byteArray[3] & 0xFFL);          //00000000_00000000_00000000_00000000_00000000_00000000_00000000_11111111
//            long longVal =
//                    (byteArray[0] & 0xFFL)
//                     | ((byteArray[1] & 0xFFL) << 8)
//                     | ((byteArray[2] & 0xFFL) << 16)
//                     | ((byteArray[3] & 0xFFL) << 24);
            return longVal;
        }

        // FUNCTION TO CREATE COMMAND PACKET
        public byte[] createCommandPacket(int cmd, long timestamp) {
            byte[] buffer = new byte[PACKET_LENGTH];

            byte[] header = new byte[3];
            header[0] = 1;
            header[1] = 2;
            header[2] = 3;

            byte[] tail = new byte[3];
            tail[0] = 4;
            tail[1] = 5;
            tail[2] = 6;

            byte cmdByte = (byte) cmd;
            byte[] timestampBytes = longToBytes(timestamp);

            System.arraycopy(header, 0, buffer, 0, 3);
            buffer[3] = cmdByte;
            System.arraycopy(timestampBytes, 0, buffer, 12, 8);
            System.arraycopy(tail, 0, buffer, 44, 3);

            return buffer;
        }

        // FUNCTION TO CREATE RETURN PACKET
        public byte[] createReturnPacket(int cmd, byte[] cliTimestampBytes, long serTimestamp, long sendbackTime, long sendRate) {
            byte[] buffer = new byte[PACKET_LENGTH];

            byte[] header = new byte[3];
            header[0] = 1;
            header[1] = 2;
            header[2] = 3;

            byte[] tail = new byte[3];
            tail[0] = 4;
            tail[1] = 5;
            tail[2] = 6;

            byte cmdByte = (byte) cmd;
//            byte[] cliTimestampBytes = longToBytes(cliTimestamp);
            byte[] serTimestampBytes = longToBytes(serTimestamp);
            byte[] sbkTimestampBytes = longToBytes(sendbackTime); // Process time
            byte[] sRTimestampBytes = longToBytes(sendRate); //time to send out a package

            System.arraycopy(header, 0, buffer, 0, 3);
            buffer[3] = cmdByte;
            System.arraycopy(cliTimestampBytes, 0, buffer, 4, 8);
            System.arraycopy(serTimestampBytes, 0, buffer, 12, 8);
            System.arraycopy(sbkTimestampBytes, 0, buffer, 20, 8);
            System.arraycopy(sRTimestampBytes,  0,buffer,  36, 8);
            System.arraycopy(tail, 0, buffer, 44, 3);

            return buffer;
        }

        // FUNCTION TO CHECK RETURN PACKET
        public boolean checkReturnPacket(byte[] recvPacket, int bufferLength) {
            return (recvPacket[0] == 1 && recvPacket[1] == 2 && recvPacket[2] == 3 && recvPacket[PACKET_LENGTH - 3] == 4 && recvPacket[PACKET_LENGTH - 2] == 5 && recvPacket[PACKET_LENGTH - 1] == 6 && bufferLength == PACKET_LENGTH);
        }

        // MAIN THREAD
        @Override
        public void run() {
            byte[] commandPacket = new byte[PACKET_LENGTH];

//            bool stillConnected = false;

            try {
                Log.d(devLabel, "Creating client thread...");
                DatagramSocket UDPSocket = new DatagramSocket(portNumber);
//                UDPSocket.setSoTimeout(1000); // 1000 ms timeout
                InetAddress socketAddress = InetAddress.getByName(ipAddress); // We assume static IP from the clients
                UDPSocket.setReuseAddress(true);
                Log.d(devLabel, "UDP socket created!");

                Log.i("ClientThread", "Socket created!");
//                Thread.sleep(200);

//                long intTime1 = SystemClock.elapsedRealtimeNanos();
//                long intTime2 = 0;

//                Toast.makeText(getApplicationContext(), "Socket created!", Toast.LENGTH_LONG).show();

                // Create a concurrent executor service to listen for incoming UDP
                ExecutorService executorRecv = Executors.newSingleThreadExecutor();
                executorRecv.execute(new Runnable() {
                    @Override
                    public void run() {
                        byte[] recvBuffer = new byte[PACKET_LENGTH];
                        DatagramPacket recvPacket = new DatagramPacket(recvBuffer, PACKET_LENGTH);

                        // while(udpSocket != null)
                        Log.i(devLabel, "Executor listening to socket!");
                        while (true) {
                            try {
                                // If global command NOT start, node is NOT in recording mode (Standby)
                                if (!startRecording && isRecording){
                                    UDPSocket.receive(recvPacket);
                                    if (recvPacket.getLength() > 0) {
                                        if (checkReturnPacket(recvPacket.getData(), recvPacket.getLength())) {
                                            // Convert DatagramPacket to bytes
                                            byte[] recvBytes = recvPacket.getData();
                                            // Update device state
                                            updateDeviceState(devLabel, recvBytes);
                                        }
                                    }
                                }
                                if (!startRecording && !isRecording) {
                                    UDPSocket.receive(recvPacket);
                                    if (recvPacket.getLength() > 0) {
                                        // Check packet integrity here
                                        if (checkReturnPacket(recvPacket.getData(), recvPacket.getLength())) {
//                                        receiveTime = System.currentTimeMillis() - startTime;
//                                        receiveTime = (SystemClock.elapsedRealtimeNanos() - sessionStartTime) / 1000; // In micros
                                            // Acknowledgement flag and reset attempt count when receive packet
                                            isConnected = true;
                                            attemptCount = 0;
                                            // Convert DatagramPacket to bytes
                                            byte[] recvBytes = recvPacket.getData();
                                            // Update device state
                                            updateDeviceState(devLabel, recvBytes);
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                isConnected = false;
                            }
                        }
                    }
                });

                // MAIN THREAD INFINITE LOOP
                while (true) {


                    // When power off command is not activated
                    if (!powerOffSystem) {
                        // Global command NOT start, node is NOT in recording state (Standby)
                        if (!startRecording && !isRecording) {
//                            if (((intTime1 - intTime2) / 1000) >= 1500000) { // 1500 ms flow control
//                                intTime2 = intTime1;
                            commandPacket = createCommandPacket(0, 0); // Keep pinging command 0
                            DatagramPacket packetToSend = new DatagramPacket(commandPacket, commandPacket.length, socketAddress, portNumber);
                            UDPSocket.send(packetToSend);
                            attemptCount++;
                            updateUIDisplay(devLabel, attemptCount, isConnected);
                            //                                Log.i("Feedback", "Pinging node " + devLabel);
                            Thread.sleep(1500); // Flow control 1500 ms
//                            }
                        }

                        // Global command start, node is NOT in recording state (When sending out Start command)
                        if (startRecording && !isRecording) {
                            for (int i = 0; i < 3; i++) {
//                                if (((intTime1 - intTime2) / 1000) >= 1000000) { // 1000 ms flow control
//                                    intTime2 = intTime1;
                                commandPacket = createCommandPacket(2, sessionTime); // Try to send file name and start recording command
                                DatagramPacket packetToSend = new DatagramPacket(commandPacket, commandPacket.length, socketAddress, portNumber);
                                UDPSocket.send(packetToSend);
                                Log.i(devLabel, "Sending file name " + sessionTime + " to node " + devLabel);
                                Thread.sleep(1000);
//                                }
                            }
//                            startTime = System.currentTimeMillis();
//                            sessionStartTime = SystemClock.elapsedRealtimeNanos() / 1000L;
                        }

                        // Global command start, node is in recording state (NTP Sync here in recording state)
                        if (startRecording && isRecording) {
//                          systemTime = System.currentTimeMillis() / 1000L;
//                            if (devLabel.equals("L")) {
//                                Thread.sleep(250);
//                            } else if (devLabel.equals("R")) {
//                                Thread.sleep(500);
//                            }

//                            Thread.sleep(500);
//                            sessionTimestamp = System.currentTimeMillis() - startTime;
//                            sessionTimestamp = (SystemClock.elapsedRealtimeNanos() / 1000L) - startTime;
//                            commandPacket = createCommandPacket(1, sessionTimestamp); // Keep pinging command 1 to keep the network alive
//                            DatagramPacket packetToSend = new DatagramPacket(commandPacket, commandPacket.length, socketAddress, portNumber);
//                            UDPSocket.send(packetToSend);
//                            attemptCount++;
//                            updateUIDisplay(devLabel, attemptCount, isConnected);
//                            Log.i(devLabel, "Node " + devLabel + " in recording state");

//                            if (packetCount > 0) {
                            byte[] recvBuffer = new byte[PACKET_LENGTH];
                            DatagramPacket recvPacket = new DatagramPacket(recvBuffer, PACKET_LENGTH);
                            startReceive = (SystemClock.elapsedRealtimeNanos() - sessionStartTime) / 1000;
                            UDPSocket.receive(recvPacket);
                            endReceive = (SystemClock.elapsedRealtimeNanos() - sessionStartTime) / 1000;
                            receiveRate = endReceive-startReceive;
                            if (recvPacket.getLength() > 0) {
                                // Check packet integrity here
                                if (checkReturnPacket(recvPacket.getData(), recvPacket.getLength())) {
                                    //                                        receiveTime = System.currentTimeMillis() - startTime;
                                    receiveTime = (SystemClock.elapsedRealtimeNanos() - sessionStartTime) / 1000; // In micros
//                                    receiveTime = SystemClock.elapsedRealtimeNanos() / 1000; // In micros
                                    // Acknowledgement flag and reset attempt count when receive packet
                                    isConnected = true;
                                    attemptCount = 0;
                                    // Convert DatagramPacket to bytes
                                    byte[] recvBytes = recvPacket.getData();

                                    // Flip bytes
                                    byte[] tempBytes = Arrays.copyOfRange(recvBytes, 4, 12);
                                    byte[] cliTimeBytes = new byte[8];
                                    for (int i = 0; i < 8; i++) {
                                        cliTimeBytes[i] = tempBytes[8 - 1 - i];
                                    }
                                    //                                            long clientTime = bytesToLong(Arrays.copyOfRange(recvBytes,4,12));
                                    byte[] returnPacket;

                                    sendbackTime = (SystemClock.elapsedRealtimeNanos() - sessionStartTime) / 1000; // In micros
//                                    sendbackTime = SystemClock.elapsedRealtimeNanos() / 1000; // In micros

                                    returnPacket = createReturnPacket(recvBytes[3], cliTimeBytes, receiveTime, sendbackTime,sendRate); // Return time request packet back to SBC, send cmd 5 back
                                    DatagramPacket packetToSend = new DatagramPacket(returnPacket, returnPacket.length, socketAddress, portNumber);
                                    startSend = (SystemClock.elapsedRealtimeNanos() - sessionStartTime) / 1000;
                                    UDPSocket.send(packetToSend);
                                    endSend = (SystemClock.elapsedRealtimeNanos() - sessionStartTime) / 1000;
                                    sendRate = endSend-startSend;
//                                    Log.i("To SBC", "Received and sending return timestamp...");

                                }
                            }
//                            }
//                            packetCount++;
                        }

                        // Global command NOT start, node is in recording state (When sending out Stop command)
                        if (!startRecording && isRecording) {
                            for (int i = 0; i < 3; i++) {
//                                if (((intTime1 - intTime2) / 1000) >= 1500000) { // 1500 ms flow control
//                                    intTime2 = intTime1;
                                commandPacket = createCommandPacket(3, 0); // Send stop command
                                DatagramPacket packetToSend = new DatagramPacket(commandPacket, commandPacket.length, socketAddress, portNumber);
                                UDPSocket.send(packetToSend);
                                Log.i(devLabel, "Stopping node " + devLabel);
                                Thread.sleep(1500);
//                                }
                            }
                        }
                    } else if (powerOffSystem && !isOff) { // When sending out Power Off command
                        for (int i = 0; i < 3; i++) {
//                            if (((intTime1 - intTime2) / 1000) >= 1000000) { // 1000 ms flow control
//                                intTime2 = intTime1;
                            commandPacket = createCommandPacket(4, 0); // Send power off command
                            DatagramPacket packetToSend = new DatagramPacket(commandPacket, commandPacket.length, socketAddress, portNumber);
                            UDPSocket.send(packetToSend);
//                                Thread.sleep(1000);
//                            }
                        }
                    } else if (isOff) {
                        // Do nothing
                    }
//                    break;

                }


            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Log.i(devLabel, "Socket creation failed!");
            }
//                }

        }

        // UPDATE UI BASED ON THE CONNECTION STATUS OF THE DEVICES
        public void updateUIDisplay(String dev, int attemptCount, boolean isConnected) {
            switch (dev) {
                case "SBC":
                    if (attemptCount <= attemptLimit && progressBar_SBC.getVisibility() != View.INVISIBLE && isConnected) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar_SBC.setVisibility(View.INVISIBLE);
                                greencheck_SBC.setVisibility(View.VISIBLE);
                                drawCheck(greencheck_SBC);
                                isConnected_SBC = true;
                            }
                        });
                    } else if (attemptCount > attemptLimit && progressBar_SBC.getVisibility() != View.VISIBLE) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar_SBC.setVisibility(View.VISIBLE);
                                greencheck_SBC.setVisibility(View.INVISIBLE);
                                isConnected_SBC = false;
                            }
                        });
                    }
                    break;
            }
        }

        // UPDATE DEVICE STATE BASED ON THE RETURN PACKET
        public void updateDeviceState(String dev, byte[] byteBuffer) {
            // Update global state variables
            switch (dev) {
                case "SBC":
                    if (byteBuffer[cmdIndex] == 2) {
                        isRecording = true;
                        isRecording_SBC = true;
                    } else if (byteBuffer[cmdIndex] == 3) {
                        isRecording = false;
                        isRecording_SBC = false;
                    } else if (byteBuffer[cmdIndex] == 4) {
                        isOff = true;
                        isOff_SBC = true;
                    }
                    break;
            }
        }
    }
//     DRAW GREEN CHECK ANIMATION TO INDICATE SUCCESS
    public void drawCheck(ImageView checkmark) {
        Drawable drawable = checkmark.getDrawable();
        if(drawable instanceof AnimatedVectorDrawableCompat) {
            AnimatedVectorDrawableCompat avdCompat = (AnimatedVectorDrawableCompat) drawable;
            avdCompat.start();
        }
        else {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) drawable;
            avd.start();
        }
    }

}