package com.kumaraswamy.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

public class ClientSide extends Thread {
    private BluetoothSocket socket;

    public ClientSide(BluetoothDevice device) {
        try {
            this.socket = device.createRfcommSocketToServiceRecord(UUID.fromString(Bluetooth.ID));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            while(true) {
                socket.connect();
                if(socket.isConnected()) {
                    Bluetooth.manager = new Manager(socket);
                    Bluetooth.manager.start();
                    Bluetooth.isConnected = true;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
