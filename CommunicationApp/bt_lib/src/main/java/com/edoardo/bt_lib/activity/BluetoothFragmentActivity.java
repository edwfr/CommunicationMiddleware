package com.edoardo.bt_lib.activity;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.edoardo.bt_lib.bluetooth.BluetoothManager;
import com.edoardo.bt_lib.database.AppDatabase;
import com.edoardo.bt_lib.database.model.Msg;
import com.edoardo.bt_lib.enums.EventType;
import com.edoardo.bt_lib.enums.Template;
import com.edoardo.bt_lib.event.BondedDevice;
import com.edoardo.bt_lib.event.ClientConnectionFail;
import com.edoardo.bt_lib.event.ClientConnectionQuitted;
import com.edoardo.bt_lib.event.ClientConnectionSuccess;
import com.edoardo.bt_lib.event.ServerConnectionFail;
import com.edoardo.bt_lib.event.ServerConnectionSuccess;
import com.edoardo.bt_lib.msg.BluetoothAck;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorEvent;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorPair;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorString;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorSubscriber;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorTemplate;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorTuple;

import java.util.Set;

import de.greenrobot.event.EventBus;

public abstract class BluetoothFragmentActivity extends FragmentActivity {

    private  static final String TAG = BluetoothFragmentActivity.class.getSimpleName();
    private BluetoothManager mBluetoothManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBluetoothManager = new BluetoothManager(this, setUUIDappIdentifier());
        checkBluetoothAvailability();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        mBluetoothManager.setNbrClientMax(myNbrClientMax());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        mBluetoothManager.closeAllConnection();
        this.unregisterReceiver(mBluetoothManager);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothManager.REQUEST_DISCOVERABLE_CODE) {
            if (resultCode == BluetoothManager.BLUETOOTH_REQUEST_REFUSED) {
                Log.d(TAG, "onActivityResult: permessi rifiutati");
            } else if (resultCode == BluetoothManager.bluetoothRequestAccepted) {
                onBluetoothStartDiscovery();
            }
        }
    }

    private void checkBluetoothAvailability(){
        if(!mBluetoothManager.isBluetoothAvailable()){
            onBluetoothNotAvailable();
        }
    }
    private void setTimeDiscoverable(int timeInSec){
        mBluetoothManager.setTimeDiscoverable(timeInSec);
    }
    private void createServer(String address){
        mBluetoothManager.createServer(address);
    }

    public boolean isConnected(){
        return mBluetoothManager.isConnected();
    }
    public BluetoothManager.TypeBluetooth getTypeBluetooth(){
        return mBluetoothManager.getmType();
    }
    public String getDeviceMacAddress(){
        return mBluetoothManager.getDeviceMacAddress();
    }

    public void scanAllBluetoothDevice(){
        mBluetoothManager.scanAllBluetoothDevice();
    }
    public void disconnect() {
        mBluetoothManager.disconnect();
    }

    public void selectServerMode(){
        setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DISCOVERY_300_SEC);
        mBluetoothManager.selectServerMode();
    }
    public void selectClientMode(){
        setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DISCOVERY_300_SEC);
        mBluetoothManager.selectClientMode();
    }

    public void createClient(String addressMac){
        Log.i(TAG, "createClient:start: " +System.currentTimeMillis());
        mBluetoothManager.createClient(addressMac);
    }

    /* SEND RECEIVE PATTERN */
    public void sendBroadcastMessage(final String message){
        mBluetoothManager.sendMessageForAll(message);
    }
    public void sendMessage(final String addressMacTarget, final String message){
        mBluetoothManager.sendMessageToTarget(new BluetoothCommunicatorPair(addressMacTarget, message));
    }

    public void receiveMsg() {
        Log.e(TAG, "receiveMsg: " +System.currentTimeMillis());
        mBluetoothManager.getMsg();
    }

    /* PUBLISH SUBSCRIBE PATTERN */
    public void publish(final String msg, final EventType e){
        BluetoothCommunicatorEvent bce = new BluetoothCommunicatorEvent(msg,e, getDeviceMacAddress());
        mBluetoothManager.publish(bce);
    }
    public void subscribe(final EventType eventType, final boolean subscribe){
        mBluetoothManager.subscribe(new BluetoothCommunicatorSubscriber(eventType, getDeviceMacAddress(), subscribe));
    }

    public Set<EventType> getAllSubscriptions() {
        return mBluetoothManager.getAllSubscription();
    }

    /* TUPLE SPACES PATTERN */
    public void in(final Template t) {
        if (isConnected()) {
            mBluetoothManager.in(t);
        }
    }
    public void rd(final Template t){
        if (isConnected()) {
            mBluetoothManager.rd(t);
        }
    }
    public void out(final String msgString, final Template t){
        if (isConnected()) {
            BluetoothCommunicatorTuple bct = new BluetoothCommunicatorTuple( (msgString), t, getDeviceMacAddress());
            mBluetoothManager.out(bct);
        }
    }

    public abstract String setUUIDappIdentifier();
    public abstract int myNbrClientMax();

    public abstract void onBluetoothDeviceFound(final BluetoothDevice device);
    public abstract void onClientConnectionSuccess();
    public abstract void onClientConnectionFail(final String serverAddress);
    public abstract void onServerConnectionSuccess();
    public abstract void onServerConnectionFail(final String clientAddress);
    public abstract void onBluetoothStartDiscovery();
    public abstract void onBluetoothMsgObjectReceived(final String message);
    public abstract void onTupleReceived(final String tuple);
    public abstract void onReceiveRequest(final String message);
    public abstract void onBluetoothNotAvailable();

    public void onEventMainThread(final BluetoothDevice device){
        if(!mBluetoothManager.isNbrMaxReached()){
            onBluetoothDeviceFound(device);
            createServer(device.getAddress());
        }
    }

    public void onEventMainThread(final ClientConnectionSuccess event){
        Log.i(TAG, "createClient:end: " +System.currentTimeMillis());
        Log.i(TAG, "onEventMainThread: CLIENT connection success");
        mBluetoothManager.onClientConnectionSuccess();
        onClientConnectionSuccess();
    }

    public void onEventMainThread(final ClientConnectionFail event){
        if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.CLIENT)) {
            Log.i(TAG, "onEventMainThread: CLIENT connection fail");
            mBluetoothManager.setIsConnectedFalse();
            onClientConnectionFail(event.serverAddress);
            disconnect();
            Log.d(TAG, "onEventMainThread: tento di riconnettere il client al server - " + event.serverAddress);
            mBluetoothManager.reconnectToLastServer(event.serverAddress);
        }
    }

    public void onEventMainThread(final ServerConnectionSuccess event){
        Log.i(TAG, "onEventMainThread: SERVER connection success");
        mBluetoothManager.setIsConnectedTrue();
        mBluetoothManager.onServerConnectionSuccess(event.getClientAddress());
        onServerConnectionSuccess();
        if (mBluetoothManager.isMsgForClient(event.getClientAddress())) {
            Log.d(TAG, "client is in dispatcher");
            while (isConnected() && mBluetoothManager.isMsgForClient(event.getClientAddress())) {
                String msg = mBluetoothManager.getMsgToDispatch(event.getClientAddress());
                mBluetoothManager.sendMessageToTarget(new BluetoothCommunicatorPair(event.getClientAddress(), msg));
            }
        }
    }

    public void onEventMainThread(final ServerConnectionFail event) {
    disconnect();
        onServerConnectionFail(event.getClientAddress());
        Log.i(TAG, "onEventMainThread: SERVER connection quitted");

    }

    public void onEventMainThread(final BluetoothCommunicatorPair msgPair){
        if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.SERVER)) {
            if (msgPair.getTargetAddress().equals(mBluetoothManager.getDeviceMacAddress())) {
                // significa che il messaggio è per il server e non devo fare altro
                AppDatabase.getInMemoryDatabase(this).daoMsg().insertMsg(new Msg(msgPair.getMsgObj().getContent()));
            } else {
                // Controllo che il client sia connesso
                // altrimenti aggiungo il msg alla lista da mandare una volta connesso
                if (mBluetoothManager.isClientConnected(msgPair.getTargetAddress())) {
                    mBluetoothManager.sendMessageToTarget(msgPair);
                } else {
                    mBluetoothManager.addMsgToDispatch(msgPair);
                }
            }
        }
    }

    public void onEventMainThread(final BluetoothCommunicatorString msgString) {
        mBluetoothManager.insertMailboxMsg(msgString);
    }

    public void onEventMainThread(final BondedDevice event){
        if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.CLIENT)) {
            onClientConnectionSuccess();
        } else {
            onServerConnectionSuccess();
        }
    }

    public void onEventMainThread(final ClientConnectionQuitted clientConnectionQuitted) {
        if (mBluetoothManager.serverContainsClientAddress(clientConnectionQuitted.getClientAddress())) {
            Log.d(TAG, "onEventMainThread: quitting connection client - " + clientConnectionQuitted.getClientAddress());
            mBluetoothManager.onServerRemoveClient(clientConnectionQuitted.getClientAddress());
            onServerConnectionFail(clientConnectionQuitted.getClientAddress());
        } else {
            Log.d(TAG, "onEventMainThread: client already removed.");
        }
    }

    public void onEventMainThread(final BluetoothCommunicatorSubscriber btCommSub) {
        if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.SERVER)) {
            if (btCommSub.isSubscribe()) {
                mBluetoothManager.onAddSubscription(btCommSub.getEventType(), btCommSub.getAddress());
            } else {
                Log.d(TAG, "onEventMainThread: isToDelete? " +btCommSub.isSubscribe());
                mBluetoothManager.onRemoveSubscription(btCommSub.getEventType(), btCommSub.getAddress());
            }
        }
    }

    public void onEventMainThread(final BluetoothCommunicatorEvent btCommEv) {
        if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.SERVER)) {
            mBluetoothManager.onPublish(btCommEv);
            if(mBluetoothManager.getAllSubscription().contains(btCommEv.getEventType())){
                onBluetoothMsgObjectReceived((btCommEv.getMsgObj().getContent().concat(" - ").concat(btCommEv.getEventType().name())));
            }
        } else {
            onBluetoothMsgObjectReceived(btCommEv.getMsgObj().getContent().concat(" - ").concat(btCommEv.getEventType().name()));
        }
    }

    public void onEventMainThread(final BluetoothCommunicatorTemplate btCommTem){
        if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.SERVER)) {
            Log.d(TAG, "onEventMainThread: addressTuple " +btCommTem.getAddress().concat("-")+(btCommTem.isToDelete()));
            BluetoothCommunicatorTuple btCommTuple = mBluetoothManager.onTemplateRequest(btCommTem);
            if (btCommTuple != null) {
                if (btCommTem.getAddress().equals(mBluetoothManager.getDeviceMacAddress())) {
                    onBluetoothMsgObjectReceived(btCommTuple.getMsgObj().getContent());
                } else {
                    mBluetoothManager.onOut(btCommTuple, btCommTem.getAddress());
                }
            }
        }
    }

    public void onEventMainThread(final BluetoothCommunicatorTuple btCommTup){
        if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.SERVER)) {
            if (mBluetoothManager.isTemplateReadRequestWaiting(btCommTup.getTemplate())) {
                manageReadRequest(btCommTup);
            }
            if (mBluetoothManager.isTemplateInRequestWaiting(btCommTup.getTemplate())) {
                manageInRequest(btCommTup);
                return;
            }
            mBluetoothManager.addTuple(btCommTup);
        } else if (getTypeBluetooth().equals(BluetoothManager.TypeBluetooth.CLIENT)) {
            onTupleReceived(btCommTup.getMsgObj().getContent());
        }
    }

    private void manageInRequest(BluetoothCommunicatorTuple btCommTup) {
        Log.d(TAG, "onEventMainThread: c'è almeno una IN pendente");
        BluetoothCommunicatorTemplate btCommTem = mBluetoothManager.getNextTemplateInRequest(btCommTup.getTemplate());
        if (btCommTem != null) {
            if (btCommTem.getAddress().equals(mBluetoothManager.getDeviceMacAddress())) {
                onTupleReceived(btCommTup.getMsgObj().getContent());
            } else {
                mBluetoothManager.onOut(btCommTup, btCommTem.getAddress());
            }
        }
    }

    private void manageReadRequest(BluetoothCommunicatorTuple btCommTup) {
        Log.d(TAG, "onEventMainThread: mando il msg a tutte le READ pendenti");
        for (BluetoothCommunicatorTemplate b : mBluetoothManager.getAllReadTemplateRequest(btCommTup.getTemplate())) {
            if (b.getAddress().equals(mBluetoothManager.getDeviceMacAddress())) {
                onTupleReceived(btCommTup.getMsgObj().getContent());
            } else {
                mBluetoothManager.onOut(btCommTup, b.getAddress());
            }
        }
    }

    public void onEventMainThread(final BluetoothAck btAck){
        Log.d(TAG, "onEventMainThread: ack received "+System.currentTimeMillis());
    }

}
