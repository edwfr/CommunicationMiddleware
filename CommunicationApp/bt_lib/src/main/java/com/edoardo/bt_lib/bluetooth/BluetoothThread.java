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
import java.util.concurrent.atomic.AtomicBoolean;


public abstract class BluetoothThread implements Runnable {

    private static final String TAG = BluetoothThread.class.getSimpleName();
    private static final int MAX_INPUT_STREAM_EXC = 100;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mSocket;
    InputStream mInputStream;
    String mMyAddressMac;
    boolean quitConnectionFromServer;
    private AtomicBoolean continueReadWrite = new AtomicBoolean(true);
    private OutputStreamWriter mOutputStreamWriter;
    private ObjectOutputStream mObjectOutputStream;
    private ObjectInputStream mObjectInputStream;
    private int mCountObjectInputStreamException;
    private AtomicBoolean mIsConnected;

    BluetoothThread(BluetoothAdapter bluetoothAdapter, Activity activity) {
        mBluetoothAdapter = bluetoothAdapter;
        mMyAddressMac = android.provider.Settings.Secure.getString(activity.getContentResolver(), "bluetooth_address");
        mIsConnected = new AtomicBoolean(false);
        mCountObjectInputStreamException = 0;
    }

    @Override
    public void run() {

        waitForConnection();

        try {

            initObjReader();

            mIsConnected.set(true);
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            if (mSocket == null) {
                return;
            }
            mOutputStreamWriter = new OutputStreamWriter(mSocket.getOutputStream());
            mObjectOutputStream = new ObjectOutputStream(mSocket.getOutputStream());

            onConnectionSuccess();

            while (this.continueReadWrite()) {

                synchronized (this) {
                    readMsgFromInputStream(bufferSize, buffer);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "===> ERROR thread bluetooth : " + e.getMessage());
            if (isConnected()) {
                onConnectionFail();
            }
            this.setIsConnectedToFalse();
        }
    }

    private void readMsgFromInputStream(int bufferSize, byte[] buffer) {
        int bytesRead;
        try {
            final StringBuilder sb = new StringBuilder();
            bytesRead = mInputStream.read(buffer);
            if (bytesRead != -1) {
                String result = "";
                while ((bytesRead == bufferSize) && (buffer[bufferSize] != 0)) {
                    result = result.concat(new String(buffer, 0, bytesRead));
                    bytesRead = mInputStream.read(buffer);
                }
                result = result.concat(new String(buffer, 0, bytesRead));
                sb.append(result);
            }
            Log.w(TAG, "run: msg received " + System.currentTimeMillis());
            JsonHelper.jsonToObject(sb.toString());
        } catch (IOException e) {
            Log.e(TAG, "===> IOException : " + e.getMessage());
            lifeline();
            if (isConnected() && (e.getMessage().contains("bt socket closed"))) {
                this.closeConnection();
                onConnectionFail();
                this.setIsConnectedToFalse();
            }
        }
    }

    private void lifeline(){
        mCountObjectInputStreamException++;
        if (mCountObjectInputStreamException > MAX_INPUT_STREAM_EXC) {
            this.setIsConnectedToFalse();
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
        try {
            if (mOutputStreamWriter != null) {
                mOutputStreamWriter.write(message);
                Log.d(TAG, "writeJSON: time: " +(System.currentTimeMillis() + " byte: " +message.length()));
                mOutputStreamWriter.flush();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    public void closeConnection() {
        if (mSocket != null) {
            try {
                this.stopReadWrite();
                mInputStream.close();
                mOutputStreamWriter.close();
                mObjectOutputStream.close();
                mObjectInputStream.close();
                mSocket.close();
                this.setIsConnectedToFalse();
            } catch (Exception e) {
                Log.e(TAG, "===+++> Connection already closed : "+e.getMessage());
            }
        }
    }

    boolean isConnected() {
        return mIsConnected.get();
    }

    boolean continueReadWrite() {
        return continueReadWrite.get();
    }

    void stopReadWrite() {
        this.continueReadWrite.set(false);
    }

    void startReadWrite() {
        this.continueReadWrite.set(true);
    }

    void setIsConnectedToFalse() {
        mIsConnected.set(false);
    }

}
