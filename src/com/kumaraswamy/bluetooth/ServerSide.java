package com.kumaraswamy.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.util.UUID;

public class ServerSide extends Thread{
    private BluetoothServerSocket serverSocket;

    public ServerSide() {
        Context context = Bluetooth.activity;
        try {
            serverSocket = Bluetooth.adapter.listenUsingRfcommWithServiceRecord(context.getApplicationInfo().loadLabel(
                    context.getPackageManager()).toString(), UUID.fromString(Bluetooth.ID));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        BluetoothSocket socket = null;

        while (socket == null) {
            try {
                Bluetooth.activity.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
                socket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(socket != null) {
                Bluetooth.isConnected = true;
                Bluetooth.manager = new Manager(socket);
                Bluetooth.manager.start();
                break;
            }
        }
    }
}
