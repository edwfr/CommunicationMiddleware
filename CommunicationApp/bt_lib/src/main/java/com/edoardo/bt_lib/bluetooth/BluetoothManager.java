package com.edoardo.bt_lib.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.edoardo.bt_lib.activity.BluetoothFragmentActivity;
import com.edoardo.bt_lib.database.AppDatabase;
import com.edoardo.bt_lib.database.model.Msg;
import com.edoardo.bt_lib.database.model.MsgDispatcher;
import com.edoardo.bt_lib.database.model.Request;
import com.edoardo.bt_lib.database.model.RoutingTable;
import com.edoardo.bt_lib.database.model.Subscription;
import com.edoardo.bt_lib.database.model.TupleSpace;
import com.edoardo.bt_lib.enums.EventType;
import com.edoardo.bt_lib.enums.Template;
import com.edoardo.bt_lib.event.BondedDevice;
import com.edoardo.bt_lib.msg.BluetoothAck;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorEvent;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorPair;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorString;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorSubscriber;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorTemplate;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorTuple;
import com.edoardo.bt_lib.msg.InvalidCallException;
import com.edoardo.bt_lib.util.Bool;
import com.edoardo.bt_lib.util.SharedPreferencesManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.greenrobot.event.EventBus;

import static android.bluetooth.BluetoothAdapter.EXTRA_SCAN_MODE;
import static android.bluetooth.BluetoothAdapter.SCAN_MODE_CONNECTABLE;


public class BluetoothManager extends BroadcastReceiver {

    public static final int REQUEST_DISCOVERABLE_CODE = 114;
    public static final int BLUETOOTH_REQUEST_REFUSED = 0;
    public static final int BLUETOOTH_TIME_DICOVERY_60_SEC = 15;
    public static final int BLUETOOTH_TIME_DICOVERY_120_SEC = 120;
    public static final int BLUETOOTH_TIME_DISCOVERY_300_SEC = 300;
    private static final String TAG = BluetoothManager.class.getSimpleName();
    private int bluetoothNbrClientMax = 7;
    private BluetoothFragmentActivity mActivity;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothClient mBluetoothClient;
    private ArrayList<String> mAdressListServerWaitingConnection;
    private HashMap<String, BluetoothServer> mServerWaitingConnectionList;
    private ArrayList<BluetoothServer> mServerConnectedList;
    private HashMap<String, Thread> mServerThreadList;
    private SerialExecutor mSerialExecutor;
    private int mNbrClientConnection;
    private int mTimeDiscoverable;
    private boolean isConnected;
    private TypeBluetooth mType;
    private String mUuidAppIdentifier;
    private Thread mThreadClient;
    private BlockingQueue<Msg> msgQueue;

    public BluetoothManager(BluetoothFragmentActivity activity, String uuidAppIdentifier) {
        mActivity = activity;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        isConnected = false;
        mNbrClientConnection = 0;
        mAdressListServerWaitingConnection = new ArrayList<>();
        mServerWaitingConnectionList = new HashMap<>();
        mServerConnectedList = new ArrayList<>();
        mServerThreadList = new HashMap<>();
        mSerialExecutor = new SerialExecutor(Executors.newSingleThreadExecutor());
        mUuidAppIdentifier = uuidAppIdentifier;

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mActivity.registerReceiver(this, intentFilter);

        IntentFilter intentFilter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mActivity.registerReceiver(this, intentFilter2);

        IntentFilter intentFilter1 = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mActivity.registerReceiver(this, intentFilter1);

        if (SharedPreferencesManager.containsType(this.mActivity)) {
            Log.d(TAG, "onCreate: shared preference presente");
            mType = loadTypeSetting();

            if (mType.equals(BluetoothManager.TypeBluetooth.CLIENT)) {
                String tempServerAddress = this.loadClientAddressSetting();
                setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DISCOVERY_300_SEC);
                if (tempServerAddress != null) {
                    this.reconnectToLastServer(tempServerAddress);
                }
            } else if (mType.equals(BluetoothManager.TypeBluetooth.SERVER)) {
                setTimeDiscoverable(BluetoothManager.BLUETOOTH_TIME_DISCOVERY_300_SEC);
                scanAllBluetoothDevice();
            }
        }

        msgQueue = new ArrayBlockingQueue<>(100);
        msgQueue.addAll(AppDatabase.getInMemoryDatabase(mActivity).daoMsg().findAllMsg());
    }

    /* START DB METHOD */
    public void addMsg(final Msg msg) {
        msgQueue.add(msg);
    }

    /* END DB METHOD */

    public void getMsg(){
        new ReceiveRequestTask().execute();
    }

    public String getCallableMsg() throws ExecutionException, InterruptedException {
        Callable<String> callable = () -> {
                Msg msg = msgQueue.take();
                AppDatabase.getInMemoryDatabase(mActivity).daoMsg().deleteMsg(msg.getUid());
                return msg.getMsg();
        };
        ExecutorService executor = Executors.newFixedThreadPool(10);
        final Future<String> result = executor.submit(callable);
        return result.get();
    }

    public void insertMailboxMsg(BluetoothCommunicatorString bcs) {
        Long id = AppDatabase.getInMemoryDatabase(mActivity).daoMsg().insertMsg(new Msg(bcs.getContent()));
        Msg msg = new Msg(bcs.getContent(), id.intValue());
        addMsg(msg);
    }

    public boolean isMsgForClient(final String clientAddress) {
        MsgDispatcher m = AppDatabase.getInMemoryDatabase(mActivity).daoMsgDispatcher().getMsgForClient(clientAddress);
        return m != null;
    }

    public String getMsgToDispatch(final String clientAddress) {
        MsgDispatcher m = AppDatabase.getInMemoryDatabase(mActivity).daoMsgDispatcher().getMsgForClient(clientAddress);
        if (m!= null) {
            AppDatabase.getInMemoryDatabase(mActivity).daoMsgDispatcher().deleteMsg(m.getUid());
            Log.d(TAG, "getMsgToDispatch: "+m.getMsg()+" "+m.getUid());
            return m.getMsg();
        }
        return null;
    }

    public void addMsgToDispatch(BluetoothCommunicatorPair msgPair) {
        MsgDispatcher m = new MsgDispatcher(msgPair.getMsgObj().getContent(), msgPair.getTargetAddress());
        AppDatabase.getInMemoryDatabase(mActivity).daoMsgDispatcher().insertMsg(m);
        Log.d(TAG, "addMsgToDispatch: insert");
    }

    public void addSubscription(final EventType e){
        if (AppDatabase.getInMemoryDatabase(mActivity).daoSubscription().isSubForClient(e.ordinal(), getDeviceMacAddress()) == null) {
            AppDatabase.getInMemoryDatabase(mActivity).daoSubscription().insertSubscription(new Subscription(e.ordinal(),getDeviceMacAddress()));
            Log.d(TAG, "addSubscription: " + new Gson().toJson(AppDatabase.getInMemoryDatabase(mActivity).daoSubscription().isSubForClient(e.ordinal(), getDeviceMacAddress())));
            Log.d(TAG, "addSubscription: insert " +System.currentTimeMillis());
        } else {
            Log.d(TAG, "addSubscription: already in db");
        }
    }

    public void removeSubscription(final EventType e){
        if (AppDatabase.getInMemoryDatabase(mActivity).daoSubscription().isSubForClient(e.ordinal(), getDeviceMacAddress()) != null) {
            Subscription s = AppDatabase.getInMemoryDatabase(mActivity).daoSubscription().isSubForClient(e.ordinal(), getDeviceMacAddress());
            AppDatabase.getInMemoryDatabase(mActivity).daoSubscription().deleteSub(s.getUid());
            Log.d(TAG, "removeSubscription: " + new Gson().toJson(AppDatabase.getInMemoryDatabase(mActivity).daoSubscription().isSubForClient(e.ordinal(), getDeviceMacAddress())));
            Log.d(TAG, "removeSubscription: done");
        } else {
            Log.d(TAG, "removeSubscription: not in db");
        }
    }

    public Set<EventType> getAllSubscription() {
        List<Subscription> l =  AppDatabase.getInMemoryDatabase(mActivity).daoSubscription().getAllSubForClient(getDeviceMacAddress());
        Set<EventType> eventSet = new HashSet<>();
        for (Subscription s: l) {
            eventSet.add(EventType.values()[s.getEventTypeId()]);
        }
        return eventSet;
    }

    public void addRoute(final EventType e, final String macSubscribers){
        if (AppDatabase.getInMemoryDatabase(mActivity).daoRoutingTable().isEventForReceiver(e.ordinal(), macSubscribers) == null) {
            AppDatabase.getInMemoryDatabase(mActivity).daoRoutingTable().insertRoute(new RoutingTable(e.ordinal(), macSubscribers));
            Log.d(TAG, "addRoute: " + new Gson().toJson(AppDatabase.getInMemoryDatabase(mActivity).daoRoutingTable().isEventForReceiver(e.ordinal(), macSubscribers)));
            Log.e(TAG, "addRoute: " +System.currentTimeMillis());
            Runnable runnable = () -> {
                if (mType.equals(TypeBluetooth.SERVER) && mServerConnectedList != null) {
                    for (BluetoothServer bluetoothServer : mServerConnectedList) {
                        if (bluetoothServer.getClientAddress().equals(macSubscribers)) {
                            bluetoothServer.writeJSON(new BluetoothAck());
                        }
                    }
                }
            };
            mSerialExecutor.execute(runnable);
        } else {
            Log.d(TAG, "addRoute: already in db");
        }
    }

    public void removeRoute(final EventType e, final String macSubscribers){
        if (AppDatabase.getInMemoryDatabase(mActivity).daoRoutingTable().isEventForReceiver(e.ordinal(), macSubscribers) != null) {
            RoutingTable r = AppDatabase.getInMemoryDatabase(mActivity).daoRoutingTable().isEventForReceiver(e.ordinal(), macSubscribers);
            AppDatabase.getInMemoryDatabase(mActivity).daoRoutingTable().deleteRouteById(r.getUid());
            Log.d(TAG, "removeRoute: " + new Gson().toJson(AppDatabase.getInMemoryDatabase(mActivity).daoRoutingTable().isEventForReceiver(e.ordinal(), macSubscribers)));
            Log.d(TAG, "removeRoute: done");
        } else {
            Log.d(TAG, "removeRoute: not in db");
        }
    }

    public Set<String> getAllRoute(final EventType eventType) {
        List<RoutingTable> l = AppDatabase.getInMemoryDatabase(mActivity).daoRoutingTable().getAllReceivers(eventType.ordinal());
        Set<String> eventSet = new HashSet<>();
        for (RoutingTable s: l) {
            eventSet.add(s.getMac());
        }
        return eventSet;
    }

    public void addTuple(BluetoothCommunicatorTuple btCommTup) {
        AppDatabase.getInMemoryDatabase(mActivity).daoTupleSpace().insertTuple(new TupleSpace(btCommTup.getTemplate().ordinal(), btCommTup.getSenderAddress(), btCommTup.getMsgObj().getContent()));
        Log.d(TAG, "addTuple: " + System.currentTimeMillis());
        Runnable runnable = () -> {
            if (mType.equals(TypeBluetooth.SERVER) && mServerConnectedList != null) {
                        for (BluetoothServer bluetoothServer : mServerConnectedList) {
                            if (bluetoothServer.getClientAddress().equals(btCommTup.getSenderAddress())) {
                                bluetoothServer.writeJSON(new BluetoothAck());
                            }
                        }
            }
        };
        mSerialExecutor.execute(runnable);
    }

    public synchronized BluetoothCommunicatorTuple safeReadTuple(final BluetoothCommunicatorTemplate btCommTem){
        if (AppDatabase.getInMemoryDatabase(mActivity).daoTupleSpace().getTuple(btCommTem.getTemplate().ordinal()) != null) {
            for (TupleSpace t : AppDatabase.getInMemoryDatabase(mActivity).daoTupleSpace().getAllTuple(btCommTem.getTemplate().ordinal())){
                if (!t.getMac().equals(btCommTem.getAddress())){
                    return new BluetoothCommunicatorTuple(new BluetoothCommunicatorString(t.getMsg()),Template.values()[t.getTemplateId()], t.getMac());
                }
            }
        }
        Log.d(TAG, "removeTuple: aggiungo template alla lista di richieste");
        addTemplateToWaitingQueue(btCommTem);
        return null;
    }

    public synchronized BluetoothCommunicatorTuple safeRemoveTuple(final BluetoothCommunicatorTemplate btCommTem){
        if (AppDatabase.getInMemoryDatabase(mActivity).daoTupleSpace().getTuple(btCommTem.getTemplate().ordinal()) != null) {
            for (TupleSpace t : AppDatabase.getInMemoryDatabase(mActivity).daoTupleSpace().getAllTuple(btCommTem.getTemplate().ordinal())){
                if (!t.getMac().equals(btCommTem.getAddress())){
                    AppDatabase.getInMemoryDatabase(mActivity).daoTupleSpace().deleteTuple(t.getUid());
                    Log.d(TAG, "safeRemoveTuple: removed");
                    return new BluetoothCommunicatorTuple(new BluetoothCommunicatorString(t.getMsg()),Template.values()[t.getTemplateId()], t.getMac());
                }
            }
        }
        Log.d(TAG, "removeTuple: aggiungo template alla lista di richieste");
        addTemplateToWaitingQueue(btCommTem);
        return null;
    }

    public synchronized BluetoothCommunicatorTuple readTuple(final BluetoothCommunicatorTemplate btCommTem){
        TupleSpace t = AppDatabase.getInMemoryDatabase(mActivity).daoTupleSpace().getTuple(btCommTem.getTemplate().ordinal());
        if (t != null) {
            Log.d(TAG, "readTuple: done");
            return new BluetoothCommunicatorTuple(new BluetoothCommunicatorString(t.getMsg()),Template.values()[t.getTemplateId()], t.getMac());
        }
        Log.d(TAG, "read: aggiungo template alla lista di richieste");
        addTemplateToWaitingQueue(btCommTem);
        return null;
    }

    public synchronized BluetoothCommunicatorTuple removeTuple(final BluetoothCommunicatorTemplate btCommTem){
        TupleSpace t = AppDatabase.getInMemoryDatabase(mActivity).daoTupleSpace().getTuple(btCommTem.getTemplate().ordinal());
        if (t != null) {
            AppDatabase.getInMemoryDatabase(mActivity).daoTupleSpace().deleteTuple(t.getUid());
            Log.d(TAG, "removeTuple: done");
            return new BluetoothCommunicatorTuple(new BluetoothCommunicatorString(t.getMsg()),Template.values()[t.getTemplateId()], t.getMac());
        }
        Log.d(TAG, "removeTuple: aggiungo template alla lista di richieste");
        addTemplateToWaitingQueue(btCommTem);
        return null;
    }

    private synchronized void addTemplateToWaitingQueue(final BluetoothCommunicatorTemplate btCommTem){
       Request r =  new Request(btCommTem.getTemplate().ordinal(),btCommTem.getAddress(), Bool.fromBool(btCommTem.isToDelete()));
       Log.d(TAG, "addTemplateToWaitingQueue: " + new Gson().toJson(r));
       AppDatabase.getInMemoryDatabase(mActivity).daoRequest().insertRequest(r);
       Log.d(TAG, "addTemplateToWaitingQueue: request add to priority queue");
        Log.d(TAG, "addTemplateToWaitingQueue: " +new Gson().toJson(AppDatabase.getInMemoryDatabase(mActivity).daoRequest().getAllRequest(btCommTem.getTemplate().ordinal())));
    }

    public List<BluetoothCommunicatorTemplate> getAllReadTemplateRequest(final Template template) {
        List<BluetoothCommunicatorTemplate> tempList = new ArrayList<>();
        HashSet<String> macSet = new HashSet<>();
        Log.d(TAG, "getAllReadTemplateRequest: dentro");
        Log.d(TAG, "getAllReadTemplateRequest: " +new Gson().toJson(AppDatabase.getInMemoryDatabase(mActivity).daoRequest().getAllReadRequest(template.ordinal())));
        for (Request r: AppDatabase.getInMemoryDatabase(mActivity).daoRequest().getAllReadRequest(template.ordinal())) {
            if (!macSet.contains(r.getMac())){
                macSet.add(r.getMac());
                tempList.add(new BluetoothCommunicatorTemplate(Template.values()[r.getTemplateId()], r.getMac(), Bool.toBool(r.getIsToDelete())));
                AppDatabase.getInMemoryDatabase(mActivity).daoRequest().deleteRequest(r.getUid());
                Log.d(TAG, "getAllReadTemplateRequest: add " +r.getMac().concat("-")+r.getIsToDelete());
            } else {
                Log.d(TAG, "getAllReadTemplateRequest: mac set already contains " +r.getMac());
            }
            Log.d(TAG, "getAllReadTemplateRequest - id: "+r.getUid());
        }
        Log.d(TAG, "getAllReadTemplateRequest: esco");
        return tempList;
    }

    public synchronized BluetoothCommunicatorTemplate getNextTemplateInRequest(final Template template){
        if (AppDatabase.getInMemoryDatabase(mActivity).daoRequest().getInRequest(template.ordinal()) != null) {
            Request r = AppDatabase.getInMemoryDatabase(mActivity).daoRequest().getInRequest(template.ordinal());
            AppDatabase.getInMemoryDatabase(mActivity).daoRequest().deleteRequest(r.getUid());
            return new BluetoothCommunicatorTemplate(Template.values()[r.getTemplateId()], r.getMac(), Bool.toBool(r.getIsToDelete()));
        }
        return null;
    }

    public boolean isTemplateReadRequestWaiting(final Template template) {
        Log.d(TAG, "isTemplateReadRequestWaiting: " + new Gson().toJson(AppDatabase.getInMemoryDatabase(mActivity).daoRequest().getAllReadRequest(template.ordinal())));
        return  (!AppDatabase.getInMemoryDatabase(mActivity).daoRequest().getAllReadRequest(template.ordinal()).isEmpty());
    }

    public boolean isTemplateInRequestWaiting(final Template template){
        return  (AppDatabase.getInMemoryDatabase(mActivity).daoRequest().getInRequest(template.ordinal()) != null);
    }

    private void saveBtType(){
        SharedPreferencesManager.setBtType(this.mActivity, getTypeBluetooth());
        Log.d(TAG, "saveBtType: done");
    }

    private void saveServerAddress(){
        if (getTypeBluetooth().equals(TypeBluetooth.CLIENT)) {
            SharedPreferencesManager.setServerAddress(this.mActivity, mBluetoothClient.getServerAddress());
        }
    }

    /* START SHARED PREFERENCES METHODS */

    private TypeBluetooth loadTypeSetting() {
        TypeBluetooth oldType = BluetoothManager.TypeBluetooth.valueOf(SharedPreferencesManager.getBtTypeValue(this.mActivity));
        Log.d(TAG, "loadSetting: done");
        Log.d(TAG, "loadSetting: type - " +oldType.toString());
        return  oldType;
    }

    @Nullable
    private String loadClientAddressSetting() {
        TypeBluetooth oldType = loadTypeSetting();
        if (oldType.equals(BluetoothManager.TypeBluetooth.CLIENT)) {
            String oldServerAddress = SharedPreferencesManager.getServerAddressValue(this.mActivity);
            Log.d(TAG, "loadSetting: done");
            Log.d(TAG, "loadSetting: server address - " +oldServerAddress);
            return  oldServerAddress;
        }
        return null;
    }

    public void selectServerMode() {
        mType = TypeBluetooth.SERVER;
    }

    public void selectClientMode() {
        mType = TypeBluetooth.CLIENT;
    }

    /* END SHARED PREFERENCES METHODS */

    private void resetMode() {
        mType = TypeBluetooth.NONE;
    }

    public String getDeviceMacAddress() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.getAddress().equals("02:00:00:00:00:00")) {
                return android.provider.Settings.Secure.getString(mActivity.getContentResolver(), "bluetooth_address");
            }
            return mBluetoothAdapter.getAddress();
        }
        return null;
    }

    public int getNbrClientConnected() {
        return this.mNbrClientConnection;
    }

    public int getNbrClientMax() {
        return bluetoothNbrClientMax;
    }

    public void setNbrClientMax(int nbrClientMax) {
        bluetoothNbrClientMax = nbrClientMax;
    }

    public boolean isNbrMaxReached() {
        return getNbrClientConnected() == getNbrClientMax();
    }

    private void incrementNbrConnection() {
        if (mNbrClientConnection < getNbrClientMax()) {
            mNbrClientConnection = mNbrClientConnection + 1;
            Log.e(TAG, "incrementNbrConnection mNbrClientConnection : " + mNbrClientConnection);
            if (isNbrMaxReached()) {
                Log.e(TAG, "incrementNbrConnection: num max reached");
                cancelDiscovery();
                resetAllOtherWaitingThreadServer();
            }
        }
    }

    private void decrementNbrConnection() {
        if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
            if (mNbrClientConnection == 0) {
                return;
            }
            mNbrClientConnection = mNbrClientConnection - 1;
            scanAllBluetoothDevice();
            if (mNbrClientConnection == 0) {
                isConnected = false;
            }
            Log.e(TAG, "decrementNbrConnection mNbrClientConnection : " + mNbrClientConnection);
        }
    }

    public void setTimeDiscoverable(int timeInSec) {
        mTimeDiscoverable = timeInSec;
    }

    public boolean isBluetoothAvailable() {
        return mBluetoothAdapter != null;
    }

    public boolean isDiscovering() {
        boolean b = mBluetoothAdapter.isDiscovering();
        Log.d(TAG, "isDiscovering: " + b);
        return b;
    }

    public boolean isDiscoverable() {
        boolean b =  mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE;
        Log.d(TAG, "isDiscoverable: " +b);
        return b;
    }

    public void cancelDiscovery() {
        if (isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    public void startDiscoverability() {
        if (isBluetoothAvailable()) {
            if (mBluetoothAdapter.isEnabled() && isDiscoverable()) {
                Log.e(TAG, "startDiscoverability: Bluetooth is already discoverable");
            } else {
                    Log.e(TAG, "startDiscoverability: Start now bluetooth Discoverability");
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, mTimeDiscoverable);
                    mActivity.startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_CODE);
                }
            }
        }

    public void startDiscovery() {
        if (isBluetoothAvailable()) {
            if (mBluetoothAdapter.isEnabled() && isDiscovering()) {
                Log.e(TAG, "Bluetooth is already discovering");
            } else {
                boolean b = mBluetoothAdapter.startDiscovery();
                Log.d(TAG, "startDiscovery: bluetooth is discovering ? " +b);
            }
        }
    }

    public void scanAllBluetoothDevice() {
        startDiscoverability();
        startDiscovery();
        Log.d(TAG, "scanAllBluetoothDevice: done");
    }

    public BluetoothManager.TypeBluetooth getTypeBluetooth() {
        return mType;
    }

    private void resetAllOtherWaitingThreadServer() {
        cancelDiscovery();
        for (Iterator<Map.Entry<String, BluetoothServer>> it = mServerWaitingConnectionList.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, BluetoothServer> bluetoothServerMap = it.next();
            if (!bluetoothServerMap.getValue().isConnected()) {
                Log.e(TAG, "resetWaitingThreadServer BluetoothServer : " + bluetoothServerMap.getKey());
                bluetoothServerMap.getValue().closeConnection();
                Thread serverThread = mServerThreadList.get(bluetoothServerMap.getKey());
                serverThread.interrupt();
                mServerThreadList.remove(bluetoothServerMap.getKey());
                it.remove();
            }
        }
    }

    public void createClient(String addressMac) {
        if (getTypeBluetooth().equals(TypeBluetooth.CLIENT)) {
            IntentFilter bondStateIntent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            mActivity.registerReceiver(this, bondStateIntent);
            mBluetoothClient = new BluetoothClient(mBluetoothAdapter, mUuidAppIdentifier, addressMac, mActivity);
            mThreadClient = new Thread(mBluetoothClient);
            mThreadClient.start();
            saveBtType();
            saveServerAddress();
        }
    }

    public void onClientConnectionSuccess(){
        if (getTypeBluetooth().equals(TypeBluetooth.CLIENT)) {
            isConnected = true;
            cancelDiscovery();
        }
    }

    public void createServer(String address) {
        if (getTypeBluetooth().equals(TypeBluetooth.SERVER) && !mAdressListServerWaitingConnection.contains(address)) {
            BluetoothServer mBluetoothServer = new BluetoothServer(mBluetoothAdapter, mUuidAppIdentifier, address, mActivity);
            Thread threadServer = new Thread(mBluetoothServer);
            threadServer.start();
            setServerWaitingConnection(address, mBluetoothServer, threadServer);
            IntentFilter bondStateIntent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            mActivity.registerReceiver(this, bondStateIntent);
            saveBtType();
        }
    }

    public void setServerWaitingConnection(String address, BluetoothServer bluetoothServer, Thread threadServer) {
        mAdressListServerWaitingConnection.add(address);
        mServerWaitingConnectionList.put(address, bluetoothServer);
        mServerThreadList.put(address, threadServer);
    }

    public void onServerConnectionSuccess(String addressClientConnected) {
        for (Map.Entry<String, BluetoothServer> bluetoothServerMap : mServerWaitingConnectionList.entrySet()) {
            if (addressClientConnected.equals(bluetoothServerMap.getValue().getClientAddress())) {
                isConnected = true;
                mServerConnectedList.add(bluetoothServerMap.getValue());
                Log.e(TAG, "===> onServerConnectionSuccess address : " + addressClientConnected);
                incrementNbrConnection();
                return;
            }
        }
    }

    public void onServerRemoveClient(String addressClientConnectionFailed) {
        int index = 0;
        for (BluetoothServer bluetoothServer : mServerConnectedList) {
            if (addressClientConnectionFailed.equals(bluetoothServer.getClientAddress())) {
                mServerConnectedList.get(index).closeConnection();
                mServerConnectedList.remove(index);
                mServerWaitingConnectionList.get(addressClientConnectionFailed).closeConnection();
                mServerWaitingConnectionList.remove(addressClientConnectionFailed);
                mServerThreadList.get(addressClientConnectionFailed).interrupt();
                mServerThreadList.remove(addressClientConnectionFailed);
                mAdressListServerWaitingConnection.remove(addressClientConnectionFailed);
                decrementNbrConnection();
                Log.i(TAG, "onServerRemoveClient address removed: " + addressClientConnectionFailed);
                return;
            }
            index++;
        }
    }

    public boolean serverContainsClientAddress(final String addressClientConnectionFailed) {
        for (BluetoothServer bluetoothServer : mServerConnectedList) {
            if (addressClientConnectionFailed.equals(bluetoothServer.getClientAddress())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void sendMessageForAll(final String message) {
        Log.e(TAG, "===> sendMessageForAll");
        Runnable runnable = () -> {
                if (mType != null && isConnected) {
                    BluetoothCommunicatorString bcs = new BluetoothCommunicatorString(message);
                    if (mServerConnectedList != null) {
                        for (BluetoothServer bluetoothServer : mServerConnectedList) {
                            bluetoothServer.writeJSON(bcs);
                        }
                    }
                    if (mBluetoothClient != null) {
                        mBluetoothClient.writeJSON(bcs);
                    }
                }
        };
        mSerialExecutor.execute(runnable);
    }

    public synchronized void sendMessageToTarget(final BluetoothCommunicatorPair message) {
        Log.e(TAG, "===> sendMessage to target: " + message.getTargetAddress());
        Log.e(TAG, "sendMessageToTarget: time: " +System.currentTimeMillis());
        Runnable runnable = () -> {
                if (mType != null && isConnected) {
                    if (mServerConnectedList != null) {
                        for (BluetoothServer bluetoothServer : mServerConnectedList) {
                            if (bluetoothServer.getClientAddress().equals(message.getTargetAddress())) {
                                bluetoothServer.writeJSON(message.getMsgObj());
                            }
                        }
                    }
                    if (mBluetoothClient != null) {
                        mBluetoothClient.writeJSON(message);
                    }
                }
        };
        mSerialExecutor.execute(runnable);
    }

    /* START send receive pattern */

    public boolean isClientConnected(final String clientAddress) {
        if (getTypeBluetooth().equals(TypeBluetooth.SERVER) && isConnected) {
            for (BluetoothServer bluetoothServer : mServerConnectedList) {
                if (bluetoothServer.getClientAddress().equals(clientAddress)) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void onAddSubscription(final EventType eventType, final String registerAddress) {
        if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
            addRoute(eventType, registerAddress);
        }
    }

    public synchronized void onRemoveSubscription(final EventType eventType, final String removeAddress) {
        if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
            removeRoute(eventType, removeAddress);
        }
    }

    /* END send receive pattern */

    /* START publish/subscribe */

    public synchronized Set<String> getAllSubscribers(final EventType eventType) throws InvalidCallException {
        if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
            return getAllRoute(eventType);
        }
        throw new InvalidCallException();
    }

    private boolean isEventForServerSafe(final BluetoothCommunicatorEvent btCommEv){
        Boolean b = getAllSubscription().contains(btCommEv.getEventType())
                && btCommEv.getSenderAddress().equals(getDeviceMacAddress());
        Log.w(TAG, "isEventForServerSafe: " +b );
        return b;
    }

    private boolean isEventForServer(final BluetoothCommunicatorEvent btCommEv){
        Boolean b = getAllSubscription().contains(btCommEv.getEventType());
        Log.w(TAG, "isEventForServer: " +b );
        return b;
    }

    public synchronized void onPublish(final BluetoothCommunicatorEvent btCommEv) {
        Log.e(TAG, "===> onPublish" + System.currentTimeMillis());

        Runnable runnable = () -> {
            if (getTypeBluetooth().equals(TypeBluetooth.SERVER) && mServerConnectedList != null) {
                        for (BluetoothServer btServer : mServerConnectedList) {
                            try {
                                if (getAllSubscribers(btCommEv.getEventType()).contains(btServer.getClientAddress())) {
                                        btServer.writeJSON(btCommEv);
                                }
                            } catch (InvalidCallException e1) {
                                Log.d(TAG, "exception: " +e1.getMessage() );
                            }
                        }
                    }
        };
        mSerialExecutor.execute(runnable);
    }

    public synchronized void onSafePublish(final BluetoothCommunicatorEvent btCommEv) {
        Log.e(TAG, "===> onSafePublish type: " +btCommEv.getEventType() +" msg: " +btCommEv.getMsgObj());
        Runnable runnable = () -> {
            if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
                    if (isEventForServerSafe(btCommEv)){
                        Log.e(TAG, "l'evento è anche per il server");
                        EventBus.getDefault().post(btCommEv.getMsgObj());
                    }
                    if (mServerConnectedList != null) {
                        for (BluetoothServer btServer : mServerConnectedList) {
                            try {
                                if (getAllSubscribers(btCommEv.getEventType()).contains(btServer.getClientAddress()) &&
                                        !btServer.getClientAddress().equals(btCommEv.getSenderAddress())) {
                                        btServer.writeJSON(btCommEv);
                                    }
                            } catch (InvalidCallException e1) {
                                Log.d(TAG, "exception: " +e1.getMessage() );
                            }
                        }
                    }
                }
        };
        mSerialExecutor.execute(runnable);
    }

    public synchronized void subscribe(final BluetoothCommunicatorSubscriber message) {
        Log.e(TAG, "===> register: " + message.getAddress() + " to type: " + message.getEventType() + "? " + message.isSubscribe());
        Log.e(TAG, "subscribe: " +System.currentTimeMillis() );
        if (getTypeBluetooth().equals(TypeBluetooth.CLIENT)) {
            Runnable runnable = () -> {
                    if (isConnected) {
                        mBluetoothClient.writeJSON(message);
                        if (message.isSubscribe()) {
                            addSubscription(message.getEventType());
                        } else {
                            removeSubscription(message.getEventType());
                        }
                    }
            };
            mSerialExecutor.execute(runnable);
        } else if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
            if (message.isSubscribe()) {
                addRoute(message.getEventType(), getDeviceMacAddress());
                addSubscription(message.getEventType());

            } else {
                removeRoute(message.getEventType(), getDeviceMacAddress());
                removeSubscription(message.getEventType());
            }
        }
    }

    public synchronized void publish(final BluetoothCommunicatorEvent event) {
        Log.d(TAG, "publish: " + event.getMsgObj() + " type: " + event.getEventType());
        Log.e(TAG, "publish: " +System.currentTimeMillis() );
        Runnable runnable = () -> {
                if (getTypeBluetooth().equals(TypeBluetooth.CLIENT) && isConnected) {
                    if (mBluetoothClient != null) {
                        mBluetoothClient.writeJSON(event);
                    }
                } else if (getTypeBluetooth().equals(TypeBluetooth.SERVER) && isConnected) {
                    EventBus.getDefault().post(event);
                }
        };
        mSerialExecutor.execute(runnable);
    }

    public synchronized void onOut(final BluetoothCommunicatorTuple btCommEv, final String targetAddress) {
        Log.d(TAG, "onOut: "+System.currentTimeMillis());
        if (isConnected) {
            Runnable runnable = () -> {
                if (getTypeBluetooth().equals(TypeBluetooth.SERVER) && mServerConnectedList != null) {
                    for (BluetoothServer btServer : mServerConnectedList) {
                        if (btServer.getClientAddress().equals(targetAddress)) {
                            btServer.writeJSON(btCommEv);
                        }
                    }
                }
            };
            mSerialExecutor.execute(runnable);
        }
    }

    public synchronized BluetoothCommunicatorTuple onTemplateRequest(final BluetoothCommunicatorTemplate btCommTem) {
        BluetoothCommunicatorTuple bct = null;
        if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
            Boolean isToDelete = btCommTem.isToDelete();
            if (isToDelete) {
                Log.d(TAG, "onTemplateRequest: " + Bool.fromBool(btCommTem.isToDelete()));
                bct = removeTuple(btCommTem);
            } else {
                Log.d(TAG, "onTemplateRequest: " + Bool.fromBool(btCommTem.isToDelete()));
                bct = readTuple(btCommTem);
            }
        }
        return bct;
    }

    /* END publish/subscribe */

    /* START tuple space */

    public synchronized void out(final BluetoothCommunicatorTuple btCommTup) {
        Log.d(TAG, "out: " + btCommTup.getMsgObj() + " from: " + btCommTup.getSenderAddress() + " template: " + btCommTup.getTemplate());
        Log.d(TAG, "out:time: "+System.currentTimeMillis());
        if (isConnected) {
            Runnable runnable = () -> {
                        if (getTypeBluetooth().equals(TypeBluetooth.CLIENT)) {
                            if (mBluetoothClient != null) {
                                mBluetoothClient.writeJSON(btCommTup);
                            }
                        } else if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
                            EventBus.getDefault().post(btCommTup);
                        }
                };
                mSerialExecutor.execute(runnable);
        }
    }

    public synchronized void rd(final Template template) {
        Log.d(TAG, "rd:time: " +System.currentTimeMillis());
        final BluetoothCommunicatorTemplate btCommTem = new BluetoothCommunicatorTemplate(template, getDeviceMacAddress(),false);
        Log.d(TAG, "rd: addressTuple " + btCommTem.getAddress());
        if (isConnected) {
            Runnable runnable = () -> {
                if (getTypeBluetooth().equals(TypeBluetooth.CLIENT)) {
                        if (mBluetoothClient != null) {
                            mBluetoothClient.writeJSON(btCommTem);
                        }
                } else if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
                        EventBus.getDefault().post(btCommTem);
                    }
            };
            mSerialExecutor.execute(runnable);
        }
    }

    public synchronized void in(final Template template) {
        Log.d(TAG, "in:time: " +System.currentTimeMillis());
        final BluetoothCommunicatorTemplate btCommTem = new BluetoothCommunicatorTemplate(template, getDeviceMacAddress(),true);
        if (isConnected) {
            Runnable runnable = () -> {
                if (getTypeBluetooth().equals(TypeBluetooth.CLIENT)) {
                        if (mBluetoothClient != null) {
                            mBluetoothClient.writeJSON(btCommTem);
                        }
                } else if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
                        EventBus.getDefault().post(btCommTem);
                    }
            };
            mSerialExecutor.execute(runnable);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: top");
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (!intent.getAction().isEmpty() && intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
            Log.d(TAG, "onReceive: ACTION_FOUND");
            Log.e(TAG, "received:start: " +System.currentTimeMillis());
            if ((mType.equals(TypeBluetooth.CLIENT) && !isConnected)
                    || (mType.equals(TypeBluetooth.SERVER) && !mAdressListServerWaitingConnection.contains(device.getAddress()))) {
                EventBus.getDefault().post(device);
                Log.e(TAG, " onReceive for BluetoothDevice ");

            }
        }
        if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
            Log.d(TAG, "onReceive: ACTION_BOND_STATE_CHANGED");
            int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
            int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
            // check for both BONDED and NONE here because in some error cases the bonding fails and we need to fail gracefully.
            if ((prevBondState == BluetoothDevice.BOND_BONDING) && (bondState == BluetoothDevice.BOND_BONDED ||
                    bondState == BluetoothDevice.BOND_NONE)) {
                    EventBus.getDefault().post(new BondedDevice(device.getAddress()));
                    Log.e(TAG, " onReceive for BluetoothDevice ");
            }
        }
        if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
            Log.e(TAG, " onReceive for BluetoothAdapter discovery finished ");
            Log.i(TAG, "onReceive: ACTION DISCOVERY FINISHED");
            if (getTypeBluetooth().equals(TypeBluetooth.SERVER)) {
                if (getNbrClientConnected() < getNbrClientMax()) {
                    Log.d(TAG, "onReceive: server-side ho ancora posto, continuo la ricerca");
                    scanAllBluetoothDevice();
                } else if (getNbrClientConnected() == getNbrClientMax()) {
                    Log.d(TAG, "onReceive: server-side ho terminato i posti, termino la ricerca");
                    cancelDiscovery();
                }
            } else if (getTypeBluetooth().equals(TypeBluetooth.CLIENT)) {
                if (isConnected) {
                    cancelDiscovery();
                    Log.d(TAG, "onReceive: client-side connected");
                } else {
                    Log.d(TAG, "onReceive: client-side non ancora connesso");
                }
            }
        }
        if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(intent.getAction())){
            Log.d(TAG, "onReceive: ACTION_SCAN_MODE_CHANGED ");
            int scanState = intent.getIntExtra(EXTRA_SCAN_MODE, -1);
            if (mType.equals(TypeBluetooth.CLIENT) && scanState == SCAN_MODE_CONNECTABLE) {
                    Log.d(TAG, "onReceive: ACTION_SCAN_MODE_CHANGED state scan " + scanState);
                Log.d(TAG, "onReceive: ACTION_SCAN_MODE_CHANGED !isDiscoverable " + !isDiscoverable());
                Log.d(TAG, "onReceive: ACTION_SCAN_MODE_CHANGED !isConnected " + !isConnected);
                    if (mBluetoothClient != null) {
                        if (mBluetoothClient.isOutOfService() && !isDiscoverable() && !isConnected) {
                            Log.d(TAG, "onReceive: " + (mBluetoothClient.isOutOfService() && !isDiscoverable() && !isConnected));
                            scanAllBluetoothDevice();
                        }
                    } else {
                        if (!isDiscoverable() && !isConnected) {
                            Log.d(TAG, "onReceive: " + (!isDiscoverable() && !isConnected));
                            scanAllBluetoothDevice();
                        }
                    }
            }
        }
    }

    public void disconnect() {
        cancelDiscovery();
        if (mType.equals(TypeBluetooth.CLIENT)) {
            Log.d(TAG, "disconnect: client");
            if (mThreadClient != null) {
                resetMode();
                cancelDiscovery();
                resetClient();
            }
        } else if (mType.equals(TypeBluetooth.SERVER)) {
            Log.d(TAG, "disconnect: server");
            if (!mServerConnectedList.isEmpty()) {
                resetMode();
                cancelDiscovery();
                resetAllThreadServer();
            }
        }
        saveBtType();
    }

    /* END tuple space */

    public void resetClient() {
        if (mBluetoothClient != null) {
            Log.d(TAG, "resetClient: close connection");
            mBluetoothClient.closeConnection();
            if(null != mThreadClient){
                mThreadClient.interrupt();
            }
            mBluetoothClient = null;
            isConnected = false;
        }
    }

    public void reconnectToLastServer(String serverAddress) {
        Log.e(TAG, "reconnectToLastServer:start: " +System.currentTimeMillis());
        startDiscoverability();
        resetClient();
        selectClientMode();
        createClient(serverAddress);
    }

    public void resetAllThreadServer(){
        for (Iterator<Map.Entry<String, BluetoothServer>> it = mServerWaitingConnectionList.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, BluetoothServer> bluetoothServerMap = it.next();
            bluetoothServerMap.getValue().closeConnection();
            Thread serverThread = mServerThreadList.get(bluetoothServerMap.getKey());
            serverThread.interrupt();
            mServerThreadList.remove(bluetoothServerMap.getKey());
            it.remove();
        }
        mServerConnectedList.clear();
        mAdressListServerWaitingConnection.clear();
        mServerWaitingConnectionList.clear();
        mServerThreadList.clear();
        mNbrClientConnection = 0;
        isConnected = false;
    }

    public void closeAllConnection() {
        cancelDiscovery();
        if (!mType.equals(TypeBluetooth.NONE)) {
            resetAllThreadServer();
            resetClient();
        }
        mBluetoothAdapter = null;
    }

    public enum TypeBluetooth {
        CLIENT,
        SERVER,
        NONE
    }

    @SuppressLint("StaticFieldLeak")
    private class ReceiveRequestTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                return getCallableMsg();
            } catch (ExecutionException e) {
                Log.e(TAG, e.getLocalizedMessage());
            } catch (InterruptedException e) {
                Log.e(TAG, e.getLocalizedMessage());
                Thread.currentThread().interrupt();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.w(TAG, "onPostExecute: ends " + System.currentTimeMillis());
            mActivity.onReceiveRequest(s);
        }
    }

}
