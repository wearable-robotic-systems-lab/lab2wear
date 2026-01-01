package com.example.dataloggerrev001;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import de.moticon.insole3_service.Insole3Service;
import de.moticon.insole3_service.proto_mobile.Common;
import de.moticon.insole3_service.proto_mobile.Service;

public class createServiceMsgReceiver_backup extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Info","broadcast receiver start...");
        String action = intent.getAction();
        Log.i("Info","Action: " + action);
        byte[] protoMsg = intent.getByteArrayExtra(Insole3Service.EXTRA_PROTO_MSG);
        try {
            Service.MoticonMessage moticonMessage = Service.MoticonMessage.parseFrom(protoMsg);
            Log.i("Info","MoticonMessage: " + moticonMessage);
            // Continue processing the received message
            switch(moticonMessage.getMsgCase()){
                case DATA_MESSAGE:
                    // HANDLE PROTO MESSAGES HERE:
                    Log.d("MyReceiver","data  message received");

                case INSOLE_ADVERTISEMENT:
                    Log.d("MyReceiver","advertisement data received");
                    Service.InsoleDevice insoleDevice = moticonMessage.getInsoleAdvertisement().getInsole();
                    String insoleAddress = insoleDevice.getDeviceAddress();
                    Common.Side insoleSide = insoleDevice.getSide();
                    int batteryLevel = moticonMessage.getInsoleAdvertisement().getBatteryLevel();
                    int insoleSize = insoleDevice.getSize();

            }
        } catch (InvalidProtocolBufferException e) {
            // Handle the exception
            Log.i("Info","Invalid Protocol Buffer");
        }

    }
}
