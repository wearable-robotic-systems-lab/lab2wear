package com.example.dataloggerrev001;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.xsens.dot.android.sdk.DotSdk;
import com.xsens.dot.android.sdk.events.DotData;
import com.xsens.dot.android.sdk.interfaces.DotDeviceCallback;
import com.xsens.dot.android.sdk.interfaces.DotScannerCallback;
import com.xsens.dot.android.sdk.models.DotDevice;
import com.xsens.dot.android.sdk.models.FilterProfileInfo;
import com.xsens.dot.android.sdk.utils.DotScanner;
import java.util.ArrayList;
import java.util.List;


public class XsensDotActivity extends AppCompatActivity implements DotDeviceCallback, DotScannerCallback {

    private DotScanner mXsScanner;
    private BluetoothAdapter bluetoothAdapter;
    boolean BLE_status = false;
    private LocationManager locationManager;
    int scannedsensor = 0;
    boolean isScanning = false;
    private List<String> mScannedSensorList = new ArrayList<>();
    private ArrayList<DotDevice> mDeviceLst = new ArrayList<>();
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 2;
    private static final int MY_PERMISSIONS_REQUEST_BACKGROUND_LOCATION = 3;
    private static final int MY_PERMISSIONS_REQUEST_CONNECT = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xsens_dot);
        Button button5 = findViewById(R.id.button5);
        Button button6 = findViewById(R.id.button6);
        Log.i("Info","Start XsnesDot...");

//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

//        checkLocationPermission();
//        checkBluetoothPermission();
//        initXsSdk();
//        initXsScanner();

        button5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isScanning = true;
//                startscan();
            }
        });

        button6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }


    private void startscan(){
        while(isScanning){
//            Log.i("Info","start scanning...");
            mXsScanner.startScan();
            if (mDeviceLst.size()==5){
                isScanning = false;
            }
        }
    }


    private void initXsSdk() {
        String version = DotSdk.getSdkVersion();
        DotSdk.setDebugEnabled(true);
        DotSdk.setReconnectEnabled(true);
    }
    private void initXsScanner() {

        mXsScanner = new DotScanner(getApplicationContext(), this);
        mXsScanner.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
    }

    @Override
    public void onDotConnectionChanged(String address, int state) {
        if (state == DotDevice.CONN_STATE_CONNECTED) {
            Log.i("Info", "Connected XsensDot: " + address);
        }
        if (state == DotDevice.CONN_STATE_DISCONNECTED) {
            Log.i("Info", "Disconnected Xsensdot: " + address);
        }
    }

    @Override
    public void onDotServicesDiscovered(String address, int status) {
        if (status == BluetoothGatt.GATT) {
            Log.i("Info", "Gatt Success");
        }
    }

    @Override
    public void onDotFirmwareVersionRead(String s, String s1) {

    }

    @Override
    public void onDotTagChanged(String s, String s1) {

    }

    @Override
    public void onDotBatteryChanged(String s, int i, int i1) {

    }

    @Override
    public void onDotDataChanged(String s, DotData dotData) {

    }

    @Override
    public void onDotInitDone(String address) {

    }

    @Override
    public void onDotButtonClicked(String s, long l) {

    }

    @Override
    public void onDotPowerSavingTriggered(String s) {

    }

    @Override
    public void onReadRemoteRssi(String s, int i) {

    }

    @Override
    public void onDotOutputRateUpdate(String s, int i) {

    }

    @Override
    public void onDotFilterProfileUpdate(String s, int i) {

    }

    @Override
    public void onDotGetFilterProfileInfo(String s, ArrayList<FilterProfileInfo> arrayList) {

    }

    @Override
    public void onSyncStatusUpdate(String s, boolean b) {

    }

    @Override
    public void onDotScanned(BluetoothDevice bluetoothDevice, int rssi) {
        Log.i("Info","device found");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Info","permission denied");
            return;
        }
        String name = bluetoothDevice.getName();
        String address = bluetoothDevice.getAddress();
        Log.i("Info","Name: "+ name + "address: "+ address);
        boolean isExist = false;
//        if (!mScannedSensorList.contains(address) & name!=null){
//        DotDevice xsDevice = new DotDevice(getApplicationContext(), bluetoothDevice, XsensDotActivity.this);
//        xsDevice.connect();
//        mDeviceLst.add(xsDevice);
        }



    private void checkBluetoothPermission(){
        boolean isBluetoothEnabled = bluetoothAdapter.isEnabled();
        boolean bluetoothAvailable = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE);
        if (!bluetoothAvailable){
            Log.i("Bluetooth", "Bluetooth is not supported");
        }
        if (bluetoothAdapter == null){
            Log.i("Info","blluetooth not available");
        }
        if (bluetoothAdapter!=null){
            Log.i("Info","not null");
            if (!isBluetoothEnabled) {
                Log.i("Info","Bluetooth dead");
                // Device doesn't support Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                    Log.i("Info", "Bluetooth is not enabled1");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_CONNECT);
                    Log.i("Info", "connect permission deny...");
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_CONNECT);
                    Log.i("Info", "connect permission deny...");
                    Log.i("Info", "Bluetooth is not enabled2");
                }

    //            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                startActivityIntent.launch(enableBtIntent);
            }

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                Log.i("Info", "Bluetooth is not enabled1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_CONNECT);
                Log.i("Info", "connect permission deny...");
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MY_PERMISSIONS_REQUEST_CONNECT);
                Log.i("Info", "connect permission deny...");
                Log.i("Info", "Bluetooth is not enabled2");
            }
        }
    }

    private void checkLocationPermission(){
        //LocationPermission
        locationManager = (LocationManager) this
                .getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Second enable FINE LOCATION ACCESS in settings.", Toast.LENGTH_LONG).show();
//            Log.i("Info", "FINE permission denied");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "First enable COARSE LOCATION ACCESS in settings.", Toast.LENGTH_LONG).show();
            Log.i("Info", "COARSE permission denied");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
            return;

        }
    }



    private ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        BLE_status = true;
                        Log.i("BLE_status: ", "enabled");
                    } else {
                        BLE_status = false;
                        Log.i("BLE_status: ", "disabled");
                    }
                }
            });

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

        if (requestCode == MY_PERMISSIONS_REQUEST_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("Info", "connect permission granted");
                // Permission was granted; you can now use location services
            } else {
                // Permission was denied; handle it accordingly
                Toast.makeText(getApplicationContext(), "Connect permission denied. You may not be able to use location-related features.", Toast.LENGTH_LONG).show();
            }

        }
    }





}