package com.edoardo.bt_lib.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

import com.edoardo.bt_lib.event.ServerConnectionFail;
import com.edoardo.bt_lib.event.ServerConnectionSuccess;
import com.edoardo.bt_lib.event.ClientConnectionQuitted;

import de.greenrobot.event.EventBus;

public class BluetoothServer extends BluetoothThread {

    private static final String TAG = BluetoothServer.class.getSimpleName();

    private UUID mUUID;
    private BluetoothServerSocket mServerSocket;
    public String mClientAddress;

    public BluetoothServer(BluetoothAdapter bluetoothAdapter, String uuiDappIdentifier, String adressMacClient, Activity activity) {
        super(bluetoothAdapter, uuiDappIdentifier, activity);
        mClientAddress = adressMacClient;
        mUUID = UUID.fromString(uuiDappIdentifier + "-" + mClientAddress.replace(":", ""));
        quitConnectionFromServer = false;
    }

    @Override
    public void waitForConnection() {
        // NOTHING TO DO IN THE SERVER
    }

    @Override
    public void initObjReader() throws IOException {
        mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("BLTServer", mUUID);
        mSocket = mServerSocket.accept();
        Log.w(TAG, "received:end: " +System.currentTimeMillis());
        mInputStream = mSocket.getInputStream();
    }

    @Override
    public void onConnectionSuccess() {
        EventBus.getDefault().post(new ServerConnectionSuccess(mClientAddress));
    }

    @Override
    public void onConnectionFail() {
        if (quitConnectionFromServer) {
        Log.d(TAG, "run: Il Client ha chiuso la connessione ");
        EventBus.getDefault().post(new ServerConnectionFail(mClientAddress));
        } else {
            Log.d(TAG, "onConnectionFail: connessione con il client " +mClientAddress +" terminata ");
            EventBus.getDefault().post(new ClientConnectionQuitted(mClientAddress));
        }
    }

    public String getClientAddress() {
        return mClientAddress;
    }


    @Override
    public void closeConnection() {
        super.closeConnection();
        try {
            mServerSocket.close();
            mServerSocket = null;
        } catch (Exception e) {
            Log.e("", "===+++> closeConnection Exception e : "+e.getMessage());
        }
    }
}