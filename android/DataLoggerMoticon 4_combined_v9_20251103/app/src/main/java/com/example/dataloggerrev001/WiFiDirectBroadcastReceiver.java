package com.example.dataloggerrev001;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

/**
 * A BroadcastReceiver that notifies of important Wi-Fi P2P events.
 * Little significant operation is done here
 */

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WiFiDirectBroadcastReceiver";

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
//    private WifiDirectActivity activity;
    private MoticonActivity activity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MoticonActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d("Feedback", "P2P enabled!");
//                Toast.makeText(context, "Wifi P2P Enabled!", Toast.LENGTH_SHORT).show();
//                activity.setIsWifiP2pEnabled(true);  // Alert the activity
            } else {
                // WiFi P2P is not enabled
                Log.d("Feedback", "P2P not enabled!");
//                Toast.makeText(context, "Wifi P2P Not Enabled!", Toast.LENGTH_SHORT).show();
//                activity.setIsWifiP2pEnabled(false);
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers

            // Request available peers from the WiFi P2P manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                manager.requestPeers(channel, activity.peerListListener);
            }
            // Need to add connection attempt here

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
//            manager.requestGroupInfo(channel, WifiP2pManager.GroupInfoListener);
            if (manager == null) {
                return;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                manager.requestConnectionInfo(channel, activity.connectionInfoListener);

                manager.requestGroupInfo(channel, activity.groupInfoListener);

//                manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
//                    @Override
//                    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
//                        Collection<WifiP2pDevice> peerList = wifiP2pGroup.getClientList();
//                        ArrayList<WifiP2pDevice> list = new ArrayList<WifiP2pDevice>(peerList);
//                        String devName;
//                        for (int i = 1; i < list.size(); i++) {
//                            devName = list.get(i).deviceName;
//                            /** transferFile here **/
//                            Log.d("Device:",devName);
//                        }
//                    }
//                });
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
//            DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
//                    .findFragmentById(R.id.frag_list);
//            fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
//                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
        }
    }
}