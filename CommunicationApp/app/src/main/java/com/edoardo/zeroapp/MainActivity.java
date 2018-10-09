package com.edoardo.zeroapp;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ToggleButton;

import com.edoardo.bt_lib.activity.BluetoothFragmentActivity;
import com.edoardo.bt_lib.bluetooth.BluetoothManager;
import com.edoardo.bt_lib.enums.EventType;
import com.edoardo.bt_lib.enums.Template;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;


public class MainActivity extends BluetoothFragmentActivity implements DiscoveredDialogFragment.DiscoveredDialogListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private final static String LgId = "50:55:27:42:C5:98";
    private final static String SamsungId = "08:3D:88:BF:D7:A1";
    private static final String CentoS = "SSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSS";
    @BindView(R.id.listView)
    ListView mListView;
    ArrayAdapter<String> mAdapter;
    List<String> mListLog;
    @BindView(R.id.send)
    Button mSendBtn;
    @BindView(R.id.client)
    ToggleButton mClientToggleBtn;
    @BindView(R.id.serveur)
    ToggleButton mServerToggleBtn;
    @BindView(R.id.connect)
    Button mConnectBtn;
    @BindView(R.id.disconnect)
    Button mDisconnectBtn;
    @BindView(R.id.receiveBtn)
    Button mReceiveButton;
    @BindView(R.id.publish)
    Button mPublishButton;
    @BindView(R.id.subscribeHigh)
    ToggleButton subscribeHigh;
    @BindView(R.id.subscribeMedium)
    ToggleButton subscribeMedium;
    @BindView(R.id.subscribeLow)
    ToggleButton subscribeLow;
    @BindView(R.id.out)
    Button mOut;
    @BindView(R.id.inA)
    Button mInA;
    @BindView(R.id.inB)
    Button mInB;
    @BindView(R.id.readA)
    Button mReadA;
    @BindView(R.id.readB)
    Button mReadB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mListLog = new ArrayList<>();
        mAdapter = new ArrayAdapter<>(this, R.layout.item_console, mListLog);
        mListView.setAdapter(mAdapter);

        //***** IMPORTANT FOR ANDROID SDK >= 6.0 *****//
        if (Build.VERSION.SDK_INT >= 23) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            } else {
                Log.d(TAG, "onCreate: permission denied");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ButterKnife.bind(this).unbind();
    }

    @Override
    public String setUUIDappIdentifier() {
        return "f520cf2c-6487-11e7-907b";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                Log.d(TAG, "onRequestPermissionsResult: " +requestCode);
            }
        }
    }

    @Override
    public int myNbrClientMax() {
        return 1;
    }

    @OnClick(R.id.serveur)
    public void serverType() {
        setLogText("===> Starting Server - MAC: " + getDeviceMacAddress());
        selectServerMode();
        updateButton();
    }

    @OnClick(R.id.client)
    public void clientType() {
        setLogText("===> Starting Client - MAC: " + getDeviceMacAddress());
        selectClientMode();
        updateButton();
    }

    @OnClick(R.id.connect)
    public void connect() {
        setLogText("===> Start Scanning devices ...");
        scanAllBluetoothDevice();
        if (getTypeBluetooth() == BluetoothManager.TypeBluetooth.Client) {
            showDiscoveredDevicesDialog();
        }
    }

    @OnClick(R.id.disconnect)
    public void disconnectButton() {
        if (getTypeBluetooth() == BluetoothManager.TypeBluetooth.Client) {
            setLogText("===> Disconnecting Client");
            mClientToggleBtn.setChecked(false);
        } else if (getTypeBluetooth() == BluetoothManager.TypeBluetooth.Server) {
            setLogText("===> Disconnecting server");
            mServerToggleBtn.setChecked(false);
        }
        super.disconnect();
        updateButton();
    }

    @OnClick(R.id.receiveBtn)
    public void receive() {
        receiveMsg();
    }

    @OnClick(R.id.send)
    public void send(){
        if (isConnected()) {
            if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.Client)) {
                if (getDeviceMacAddress().equals(LgId)) {
                    sendMessage(LgId,CentoS);
                    setLogText("===> Send: 'L' to LG");
                } else if (getDeviceMacAddress().equals(SamsungId)) {
                    sendMessage(SamsungId,"X");
                    setLogText("===> Send: 'S' to SAMSUNG");
                }
            } else if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.Server)) {
                sendBroadcastMessage("X");
                setLogText("===> Send: 'X' broadcast");
            }
        }

    }

    @OnClick(R.id.publish)
    public void publishMsg(){
        if (isConnected()) {
            if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.Client)) {
                if (getDeviceMacAddress().equals(SamsungId)) {
                    publish(CentoS, EventType.HIGH);
                    setLogText("===> Publish: 'S' type HIGH");
                } else if (getDeviceMacAddress().equals(LgId)) {
                    publish("S", EventType.MEDIUM);
                    setLogText("===> Publish: 'L' type MEDIUM");
                }
            } else if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.Server)) {
                publish("X", EventType.LOW);
                setLogText("===> Publish: 'X' type LOW");
            }
        }
    }

    @OnCheckedChanged(R.id.subscribeHigh)
    public void subHigh(boolean checked){
        if (isConnected()) {
            if (checked) {
                subscribe(EventType.HIGH, true);
                setLogText("===> Device subscribe to HIGH type ");
            } else {
                subscribe(EventType.HIGH, false);
                setLogText("===> Device remove to HIGH type ");
            }
        }
    }

    @OnCheckedChanged(R.id.subscribeMedium)
    public void subMed(boolean checked){
        if (isConnected()) {
            if (checked) {
                subscribe(EventType.MEDIUM, true);
                setLogText("===> Device subscribe to MEDIUM type ");
            } else {
                subscribe(EventType.MEDIUM, false);
                setLogText("===> Device remove to MEDIUM type ");
            }
        }
    }

    @OnCheckedChanged(R.id.subscribeLow)
    public void subLow(boolean checked){
        if (isConnected()) {
            if (checked) {
                subscribe(EventType.LOW, true);
                setLogText("===> Device subscribe to LOW type ");
            } else {
                subscribe(EventType.LOW, false);
                setLogText("===> Device remove to LOW type ");
            }
        }
    }

    @OnClick(R.id.out)
    public void outMsg(){
        if (isConnected()) {
            if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.Client)) {
                if (getDeviceMacAddress().equals(LgId)) {
                    out(CentoS, Template.TEMPLATE_A);
                    setLogText("===> Out: '1xS' TEMPLATE A");
                } else if (getDeviceMacAddress().equals(SamsungId)) {
                    out("S", Template.TEMPLATE_B);
                    setLogText("===> Out: 'S' TEMPLATE B");
                }
            } else if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.Server)) {
                out("X", Template.TEMPLATE_A);
                setLogText("===> Out: 'X' TEMPLATE A");
            }
        }
    }

    @OnClick(R.id.inA)
    public void inA(){
        in(Template.TEMPLATE_A);
    }

    @OnClick(R.id.inB)
    public void inB(){
        in(Template.TEMPLATE_B);
    }

    @OnClick(R.id.readA)
    public void rdA(){
        rd(Template.TEMPLATE_A);
    }

    @OnClick(R.id.readB)
    public void rdB(){
            rd(Template.TEMPLATE_B);
    }

    private void updateButton(){

        if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.Client)) {
            mConnectBtn.setText("CONN");
            mServerToggleBtn.setChecked(false);
            mClientToggleBtn.setChecked(true);
            subscribeHigh.setEnabled(false);
            subscribeMedium.setEnabled(false);
            subscribeLow.setEnabled(false);
            if (isConnected()) {
                mPublishButton.setEnabled(true);
                mSendBtn.setEnabled(true);
                mReceiveButton.setEnabled(true);
                mConnectBtn.setEnabled(false);
                mDisconnectBtn.setEnabled(true);
                subscribeHigh.setEnabled(true);
                subscribeMedium.setEnabled(true);
                subscribeLow.setEnabled(true);
                for (EventType e: getAllSubscriptions()){
                    if (e.equals(EventType.HIGH)){
                        subscribeHigh.setChecked(true);
                    } else if (e.equals(EventType.MEDIUM)){
                        subscribeMedium.setChecked(true);
                    } else if (e.equals(EventType.LOW)){
                        subscribeLow.setChecked(true);
                    }
                }
                mOut.setEnabled(true);
                mReadA.setEnabled(true);
                mReadB.setEnabled(true);
                mInA.setEnabled(true);
                mInB.setEnabled(true);
            } else {
                mClientToggleBtn.setEnabled(true);
                mServerToggleBtn.setEnabled(true);
                mSendBtn.setEnabled(false);
                mConnectBtn.setEnabled(true);
                mDisconnectBtn.setEnabled(false);
                subscribeHigh.setEnabled(false);
                subscribeMedium.setEnabled(false);
                subscribeLow.setEnabled(false);
                mPublishButton.setEnabled(false);
                mReceiveButton.setEnabled(false);
                mOut.setEnabled(false);
                mReadA.setEnabled(false);
                mReadB.setEnabled(false);
                mInA.setEnabled(false);
                mInB.setEnabled(false);
                Log.d(TAG, "updateButton: sono disconnesso");
            }
        }
        if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.Server)) {
            mConnectBtn.setText("SCAN");
            mServerToggleBtn.setChecked(true);
            mClientToggleBtn.setChecked(false);
            mServerToggleBtn.setEnabled(false);
            mClientToggleBtn.setEnabled(false);
            subscribeHigh.setEnabled(false);
            subscribeMedium.setEnabled(false);
            subscribeLow.setEnabled(false);
            mReceiveButton.setEnabled(true);
            if (isConnected()) {
                mPublishButton.setEnabled(true);
                mSendBtn.setEnabled(true);
                mConnectBtn.setEnabled(false);
                mDisconnectBtn.setEnabled(true);
                subscribeHigh.setEnabled(true);
                subscribeMedium.setEnabled(true);
                subscribeLow.setEnabled(true);
                for (EventType e: getAllSubscriptions()){
                    if (e.equals(EventType.HIGH)){
                        subscribeHigh.setChecked(true);
                    } else if (e.equals(EventType.MEDIUM)){
                        subscribeMedium.setChecked(true);
                    } else if (e.equals(EventType.LOW)){
                        subscribeLow.setChecked(true);
                    }
                }
                mOut.setEnabled(true);
                mReadA.setEnabled(true);
                mReadB.setEnabled(true);
                mInA.setEnabled(true);
                mInB.setEnabled(true);
            } else {
                mClientToggleBtn.setEnabled(true);
                mServerToggleBtn.setEnabled(true);
                mSendBtn.setEnabled(false);
                mConnectBtn.setEnabled(true);
                mDisconnectBtn.setEnabled(false);
                subscribeHigh.setEnabled(false);
                subscribeMedium.setEnabled(false);
                subscribeLow.setEnabled(false);
                mPublishButton.setEnabled(false);
                mReceiveButton.setEnabled(false);
                mOut.setEnabled(false);
                mReadA.setEnabled(false);
                mReadB.setEnabled(false);
                mInA.setEnabled(false);
                mInB.setEnabled(false);
            }
        }
        if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.None)){
            mServerToggleBtn.setEnabled(true);
            mClientToggleBtn.setEnabled(true);
            mSendBtn.setEnabled(false);
            mReceiveButton.setEnabled(false);
            mConnectBtn.setEnabled(false);
            mDisconnectBtn.setEnabled(false);
            subscribeHigh.setEnabled(false);
            subscribeMedium.setEnabled(false);
            subscribeLow.setEnabled(false);
            mPublishButton.setEnabled(false);
            mOut.setEnabled(false);
            mReadA.setEnabled(false);
            mReadB.setEnabled(false);
            mInA.setEnabled(false);
            mInB.setEnabled(false);
        }
    }

    @Override
    public void onBluetoothStartDiscovery() {
        setLogText("===> Start discovering ! Your mac address : " + getDeviceMacAddress());
    }

    @Override
    public void onBluetoothMsgObjectReceived(final String o) {
            setLogText("===> Receive: '" + o +"'");
    }

    @Override
    public void onBluetoothDeviceFound(final BluetoothDevice device) {
        if(getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.Server)) {
            setLogText("===> Device detected and Thread Server created for this address : " + device.getAddress());
        }else{
            setLogText("===> Device detected : "+ device.getAddress());
        }
    }

    @Override
    public void onClientConnectionSuccess() {
        setLogText("===> Client Connection success !");
        updateButton();
    }

    @Override
    public void onClientConnectionFail(final String serverAddress) {
        setLogText("===> Client disconnect from server " + serverAddress);
        updateButton();
    }

    @Override
    public void onServerConnectionSuccess() {
        setLogText("===> Server Connection success !");
        updateButton();
    }

    @Override
    public void onServerConnectionFail(String clientAddress) {
        setLogText("===> Server Connection to client " + clientAddress +" removed !");
        updateButton();
    }

    @Override
    public void onBluetoothNotAvailable() {
        setLogText("===> Bluetooth not available on this device");
    }

    public void setLogText(final String text) {
        mListLog.add(text);
        mAdapter.notifyDataSetChanged();
        mListView.setSelection(mListView.getCount() - 1);
    }

    private void showDiscoveredDevicesDialog() {
        String tag = DiscoveredDialogFragment.class.getSimpleName();
        DiscoveredDialogFragment fragment = DiscoveredDialogFragment.newInstance();
        fragment.setListener(this);
        showDialogFragment(fragment, tag);
    }

    private void showDialogFragment(DialogFragment dialogFragment, String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(dialogFragment, tag);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onDeviceSelectedForConnection(String addressMac) {
        setLogText("===> Connecting to " + addressMac);
        createClient(addressMac);
    }

    @Override
    public void onScanClicked() {
        scanAllBluetoothDevice();
    }

    @Override
    public void onTupleReceived(String tuple) {
        onBluetoothMsgObjectReceived(tuple);
    }

    @Override
    public void onReceiveRequest(String message) {
        onBluetoothMsgObjectReceived(message);
    }
}
