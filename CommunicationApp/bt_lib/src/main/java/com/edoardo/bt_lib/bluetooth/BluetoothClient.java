package com.edoardo.bt_lib.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.edoardo.bt_lib.event.ClientConnectionFail;
import com.edoardo.bt_lib.event.ClientConnectionSuccess;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;

public class BluetoothClient extends BluetoothThread {

    private static final String TAG = BluetoothClient.class.getSimpleName();

    private UUID mUUID;
    private String mServerAddress;
    private AtomicBoolean outOfService;

    private AtomicBoolean keepTryingConnection;

    BluetoothClient(BluetoothAdapter bluetoothAdapter, String uuidAppIdentifier, String addressMacServer, Activity activity) {
        super(bluetoothAdapter, activity);
        mServerAddress = addressMacServer;
        mUUID = UUID.fromString(uuidAppIdentifier + "-" + mMyAddressMac.replace(":", ""));
        keepTryingConnection = new AtomicBoolean(false);
        this.startTryingConnection();
        outOfService = new AtomicBoolean(false);
    }

    @Override
    public void waitForConnection(){

        BluetoothDevice mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mServerAddress);

        while ((mInputStream == null) && continueReadWrite() && keepTryingConnection()) {
            BluetoothConnector mBluetoothConnector = new BluetoothConnector(mBluetoothDevice, mBluetoothAdapter, mUUID);

            try {
                mSocket = mBluetoothConnector.connect();
                Log.w(TAG, "reconnectToLastServer:done " +System.currentTimeMillis());
                mInputStream = mSocket.getInputStream();
                outOfService.set(false);
            } catch (IOException e1) {
                Log.d(TAG, "waitForConnection: isOutOfService");
                outOfService.set(true);
                Log.e(TAG,"===> mSocket IOException : "+ e1.getMessage());
            }
        }

        if (mSocket == null) {
            Log.e(TAG, "===> mSocket is null");
        }
    }

    boolean isOutOfService() {
        Log.d(TAG, "isOutOfService: "+outOfService.get());
        return outOfService.get();
    }

    @Override
    public void initObjReader() throws IOException {
        // do nothing
    }

    @Override
    public void onConnectionSuccess() {
        EventBus.getDefault().post(new ClientConnectionSuccess());
    }

    @Override
    public void onConnectionFail() {
        EventBus.getDefault().post(new ClientConnectionFail(mServerAddress));
    }

    String getServerAddress() {
        return this.mServerAddress;
    }

    @Override
    public void closeConnection() {
        this.stopTryingConnection();
        super.closeConnection();
    }

    private void startTryingConnection() {
        this.keepTryingConnection.set(true);
    }

    private void stopTryingConnection() {
        this.keepTryingConnection.set(false);
    }

    private boolean keepTryingConnection() {
        return this.keepTryingConnection.get();
    }
}
