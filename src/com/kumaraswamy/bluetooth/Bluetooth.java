package com.kumaraswamy.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.AsynchUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@UsesPermissions(permissionNames = "android.permission.BLUETOOTH, " +
        "android.permission.BLUETOOTH_ADMIN," +
        "android.permission.ACCESS_COARSE_LOCATION, " +
        "android.permission.ACCESS_FINE_LOCATION, " +
        "android.permission.WRITE_EXTERNAL_STORAGE, " +
        "android.permission.READ_EXTERNAL_STORAGE")
@DesignerComponent(version = 1,
        category = ComponentCategory.EXTENSION,
        description = "Send data through Bluetooth, made by Kumaraswamy",
        nonVisible = true,
        iconName = "aiwebres/icon.png")

@SimpleObject(external = true)
public class Bluetooth extends AndroidNonvisibleComponent {

    public static Activity activity = null;
    private BluetoothDevice[] devices;

    public static BluetoothAdapter adapter;
    public static Manager manager;

    public static boolean isConnected;

    private final String defaultID = "8ce255c0-223a-11e0-ac64-0803450c9a66",
                         defaultFolder = "/storage/emulated/0/BluetoothTransfer/";
    public static String ID, folder;

    public static int messages = 0, receivedLength = 0;
    private int previousCounted = 0;
    public static String dataReceived;

    public static boolean isMessage;

    public Bluetooth(ComponentContainer container) {
        super(container.$form());
        activity = container.$context();

        ID = defaultID;
        folder = defaultFolder;
    }

    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_TEXTAREA,
            defaultValue = defaultID)
    @SimpleProperty(
            description = "Set the UUID needed for Bluetooth")
    public void UUID(String newID) {
        ID = newID;
    }

    @SimpleProperty(
            description = "Returns the default / set UUID")
    public String UUID() {
        return ID;
    }

    @DesignerProperty(
            editorType = PropertyTypeConstants.PROPERTY_TYPE_TEXTAREA,
            defaultValue = defaultFolder)
    @SimpleProperty(
            description = "Set the path to receive files")
    public void path(String folder) {
        this.folder = folder;
    }

    @SimpleProperty(
            description = "Path where files will be saved")
    public String path() {
        if(folder == null) return "";
        else return folder;
    }

    @SimpleFunction(
            description = "Search for nearby devices")
    public void AvailableDevices() {
        adapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> devices = adapter.getBondedDevices();
        this.devices =new BluetoothDevice[devices.size()];

        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivity(enableIntent);

        ArrayList<String> namesList  = new ArrayList<>();
        ArrayList<String> devicesList  = new ArrayList<>();

        int i = 0;
        for (BluetoothDevice device : devices) {
            namesList.add(device.getName());
            devicesList.add(device.getAddress());
            this.devices[i] = device;
            i++;
        }

        FoundDevices(namesList, devicesList);
    }

    @SimpleFunction(
            description = "Connect via index")
    public void Connect(int device) {
        device--;
        ClientSide client = new ClientSide(devices[device]);
        client.start();
    }

    @SimpleFunction(
            description = "Search for connection")
    public void AcceptConnection() {
        ServerSide serverSide = new ServerSide();
        serverSide.start();
        connectionListener();
    }

    @SimpleFunction(
            description = "Start the message listener")
    public void MessageListener() {
        messageListener();
    }

    @SimpleFunction(
            description = "Send message to the connected device")
    public void SendMessage(String message) {
        byte[] data = message.getBytes();
        writeData((data.length + "!").getBytes(), data);
    }

    @SimpleFunction(
            description = "Send document to connected device")
    public void SendDocument(String name) {
        try {
            FileInputStream data = new FileInputStream(name);
            byte[] fileData = new byte[data.available()];
            data.read(fileData);
            data.close();
            writeData((fileData.length + " "  + new File(name).getName()).getBytes(), fileData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SimpleEvent(
            description = "Raised when found nearby devices")
    public void FoundDevices(List<String> names, List<String> addresses) {
        EventDispatcher.dispatchEvent(this, "FoundDevices", names, addresses);
    }

    @SimpleEvent(
            description = "Raised when device is connected")
    public void Connected() {
        EventDispatcher.dispatchEvent(this, "Connected");
    }

    @SimpleEvent(
            description = "Raised when device is connected")
    public void ReceivedMessage(boolean isMessage, String data, int length) {
        EventDispatcher.dispatchEvent(this, "ReceivedMessage", isMessage, data, length);
    }

    @SimpleEvent(
            description = "Raised when progress changed while sending documents or message")
    public void SendingData(long sentBytes, long totalBytes) {
        EventDispatcher.dispatchEvent(this, "SendingData", sentBytes, totalBytes);
    }

    @SimpleEvent(
            description = "Raised when sent document or message")
    public void SentData(boolean isMessage) {
        EventDispatcher.dispatchEvent(this, "SentData", isMessage);
    }

    private void writeData(final byte[] details, final byte[] data) {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                int subDataSize = 500, sentData = 0;
                manager.write(details);

                for(int i=0; i <data.length; i += subDataSize){
                    byte[] bytes = Arrays.copyOfRange(data, i, Math.min(data.length, i + subDataSize));
                    sentData = sentData + bytes.length;
                    manager.write(bytes);
                    final int finalSentBytes = sentData;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SendingData(finalSentBytes, data.length);
                        }
                    });
                }

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SentData(new String(details).contains("!"));
                    }
                });
            }
        });
    }

    private void connectionListener() {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if(isConnected) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Connected();
                            }
                        });
                        break;
                    }
                }
            }
        });
    }

    public void messageListener() {
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if(messages != previousCounted) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ReceivedMessage(isMessage, dataReceived, receivedLength);
                            }
                        });
                        previousCounted++;
                    }
                }
            }
        });
    }
}
