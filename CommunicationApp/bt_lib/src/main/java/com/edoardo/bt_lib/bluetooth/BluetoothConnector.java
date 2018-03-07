package com.edoardo.bt_lib.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class BluetoothConnector {

    private final static String TAG = BluetoothConnector.class.getSimpleName();
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private BluetoothAdapter bluetoothAdapter;
    private List<UUID> uuidCandidates;
    private int candidate;
    private UUID my_uuid;

    protected BluetoothConnector(BluetoothDevice bluetoothDevice, BluetoothAdapter bluetoothAdapter, UUID uuid) {
        this.bluetoothDevice = bluetoothDevice;
        this.bluetoothAdapter = bluetoothAdapter;
        this.uuidCandidates = new ArrayList<>();
        this.uuidCandidates.add(uuid);
        my_uuid = uuid;
    }
    
    protected BluetoothSocket connect() throws IOException {
        boolean success = false;
        while(selectSocket()) {
            bluetoothAdapter.cancelDiscovery();
            try {
                bluetoothSocket.connect();
                success = true;
                break;
            } catch (Exception e) {
                Log.e(TAG, "connecting exception: " +e.getMessage() );
            }
        }
        if (!success) {
            throw new IOException("Can not connect to device " +bluetoothDevice.getAddress());
        }

        return bluetoothSocket;
    }

    private boolean selectSocket() throws IOException {
        if (candidate >= uuidCandidates.size()) {
            return false;
        }

        UUID uuid = uuidCandidates.get(candidate++);

        Log.d(TAG, "Connecting to Protocol: " + uuid);
        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);

        return true;

    }

    private void connectSocket() {

        BluetoothSocket tmp = null;

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            tmp = bluetoothDevice.createRfcommSocketToServiceRecord(my_uuid);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        bluetoothSocket = tmp;

    }
}
