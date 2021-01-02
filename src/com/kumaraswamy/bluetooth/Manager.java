package com.kumaraswamy.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Manager extends Thread {
    private final BluetoothSocket socket;
    private InputStream input;
    private OutputStream output;

    private byte[] buffer;
    private boolean isReceiving = true;
    private boolean isMessage = false;
    private String fileName = "";
    private int bytesNeeded = -1, received = 0;;

    public Manager(BluetoothSocket socket) {
        this.socket = socket;
        InputStream input = null;
        OutputStream output = null;

        try {
            input = this.socket.getInputStream();
            output = this.socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.input = input;
        this.output = output;
    }

    public void run() {
        while (true) {
            if(isReceiving) {
                try {
                    byte[] receiveData = new byte[input.available()];
                    if(input.read(receiveData) > 0) {
                        String length = new String(receiveData);

                        if(length.contains("!")) {
                            isMessage = true;
                            bytesNeeded = Integer.parseInt(length.replaceAll("!", ""));
                        } else {
                            String[] details = length.split(" ");
                            bytesNeeded = Integer.parseInt(details[0]);
                            fileName = details[1];
//                            Log.e("File name XOMA received : ", fileName);
                        }

//                        Log.e("Building byte array", String.valueOf(bytesNeeded));
                        buffer = new byte[bytesNeeded];
                        isReceiving = false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    byte[] tempData = new byte[input.available()];
                    int nowReceived = input.read(tempData);
                    System.arraycopy(tempData,0,buffer,received,nowReceived);

                    received += nowReceived;

                    if(bytesNeeded == received) {
                        isReceiving = true;
                        received = 0;
                        bytesNeeded = 0;

//                        Log.e("IS FILE :", String.valueOf(isMessage));

                        if(isMessage) {
                            Bluetooth.dataReceived = new String(buffer);
                            Bluetooth.isMessage = true;
                            isMessage = false;
                        } else {
//                            Log.e("File name built : ", fileName);
                            Bluetooth.activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String path = Bluetooth.folder + fileName;
                                    try {
                                        FileOutputStream outputStream1 = new FileOutputStream(path);
                                        outputStream1.write(buffer);
                                        outputStream1.close();
                                        Bluetooth.isMessage = false;
                                        Bluetooth.dataReceived = path;
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        isMessage = false;
                        Bluetooth.receivedLength = buffer.length;
                        Bluetooth.messages++;

//                        Log.e("BUILDING DATA", new String(buffer));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void write(byte[] data) {
        try {
            output.write(data);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
