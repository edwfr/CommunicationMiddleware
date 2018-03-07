package com.edoardo.bt_lib.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.edoardo.bt_lib.util.JsonHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;


public abstract class BluetoothThread implements Runnable {

    private static final String TAG = BluetoothThread.class.getSimpleName();

    public boolean CONTINUE_READ_WRITE = true;

    public String mUuiDappIdentifier;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothSocket mSocket;
    public InputStream mInputStream;
    public String mMyAddressMac;
    private OutputStreamWriter mOutputStreamWriter;
    private ObjectOutputStream mObjectOutputStream;
    private ObjectInputStream mObjectInputStream;
    private int mCountObjectInputStreamExection;
    private boolean mIsConnected;
    boolean quitConnectionFromServer;

    public BluetoothThread(BluetoothAdapter bluetoothAdapter, String uuiDappIdentifier, Activity activity) {
        mBluetoothAdapter = bluetoothAdapter;
        mUuiDappIdentifier = uuiDappIdentifier;
        mMyAddressMac = android.provider.Settings.Secure.getString(activity.getContentResolver(), "bluetooth_address");
        mIsConnected = false;
        mCountObjectInputStreamExection = 0;
    }

    @Override
    public void run() {

        waitForConnection();

        try {

            initObjReader();

            mIsConnected = true;
            int bufferSize = 1024;
            int bytesRead;
            byte[] buffer = new byte[bufferSize];

            if(mSocket == null) return;
            mOutputStreamWriter = new OutputStreamWriter(mSocket.getOutputStream());
            if(mSocket == null) return;
            mObjectOutputStream = new ObjectOutputStream(mSocket.getOutputStream());

//            mOutputStreamWriter.flush();
//            mObjectOutputStream.flush();

            onConnectionSuccess();

//            writeJSON(new Object());

            while (CONTINUE_READ_WRITE) {

                synchronized (this) {
                    try {
                        final StringBuilder sb = new StringBuilder();
                        if(mInputStream == null) return;
                        bytesRead = mInputStream.read(buffer);
                        if (bytesRead != -1) {
                            String result = "";
                            while ((bytesRead == bufferSize) && (buffer[bufferSize] != 0)) {
                                result = result.concat(new String(buffer, 0, bytesRead));
                                if(mInputStream == null) return;
                                bytesRead = mInputStream.read(buffer);
                            }
                            result = result + new String(buffer, 0, bytesRead);
                            sb.append(result);
                        }
                        Log.w(TAG, "run: msg received " +System.currentTimeMillis());
                        JsonHelper.jsonToObject(sb.toString());
                    } catch (IOException e) {
                        Log.e(TAG, "===> IOException : " + e.getMessage());
                        lifeline();
                        if(mIsConnected && (e.getMessage().contains("bt socket closed"))){
                            this.closeConnection();
                            onConnectionFail();
                            mIsConnected = false;
                        }
                    }

                }
            }
        } catch (IOException e) {
            Log.e(TAG, "===> ERROR thread bluetooth : " + e.getMessage());
            e.printStackTrace();
            if (mIsConnected) {
                onConnectionFail();
            }
            mIsConnected = false;
        }
    }

    private void lifeline(){
        mCountObjectInputStreamExection++;
        if(mCountObjectInputStreamExection>100){
            CONTINUE_READ_WRITE = false;
            if (isConnected()) {
                this.closeConnection();
            }
        }
    }

    public abstract void waitForConnection();

    public abstract void initObjReader() throws IOException;

    public abstract void onConnectionSuccess();

    public abstract void onConnectionFail();

    void writeJSON(Object objToWrite) {
        String message = JsonHelper.objectToJsonString(objToWrite);
//        Log.d(TAG, "writeJSON:1: message send" +message+" time: " +System.currentTimeMillis());
//        Log.d(TAG, "writeJSON:1: message time: " +System.currentTimeMillis());
//        Log.d(TAG, "writeJSON: message Byte: " +message.length());
        try {
            if (mOutputStreamWriter != null) {
                mOutputStreamWriter.write(message);
                Log.d(TAG, "writeJSON: time: " +(System.currentTimeMillis() + " byte: " +message.length()));
                mOutputStreamWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        if (mSocket != null) {
            try {
                CONTINUE_READ_WRITE = false;
                mInputStream.close();
                mInputStream = null;
                mOutputStreamWriter.close();
                mOutputStreamWriter = null;
                mObjectOutputStream.close();
                mObjectOutputStream = null;
                mObjectInputStream.close();
                mObjectInputStream = null;
                mSocket.close();
                mSocket = null;
                mIsConnected = false;
            } catch (Exception e) {
                Log.e(TAG, "===+++> Connection already closed : "+e.getMessage());
            }
        }
    }

    boolean isConnected() {
        return mIsConnected;
    }

}
