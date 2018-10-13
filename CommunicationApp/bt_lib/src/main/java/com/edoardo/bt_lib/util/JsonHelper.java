package com.edoardo.bt_lib.util;

import android.util.Log;

import com.edoardo.bt_lib.enums.MsgType;
import com.edoardo.bt_lib.event.ClientConnectionQuitted;
import com.edoardo.bt_lib.msg.BluetoothAck;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorEvent;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorPair;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorString;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorSubscriber;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorTemplate;
import com.edoardo.bt_lib.msg.BluetoothCommunicatorTuple;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import de.greenrobot.event.EventBus;

import static com.edoardo.bt_lib.enums.MsgType.BT_ACK;
import static com.edoardo.bt_lib.enums.MsgType.EVENT_MSG;
import static com.edoardo.bt_lib.enums.MsgType.QUIT_CLIENT_MSG;
import static com.edoardo.bt_lib.enums.MsgType.STRING_MSG;
import static com.edoardo.bt_lib.enums.MsgType.SUBSCRIBE_MSG;
import static com.edoardo.bt_lib.enums.MsgType.TARGET_PAIR_MSG;
import static com.edoardo.bt_lib.enums.MsgType.TUPLE_MSG;
import static com.edoardo.bt_lib.enums.MsgType.TUPLE_TEMPLATE_MSG;

public class JsonHelper {

    private static final String TAG = JsonHelper.class.getSimpleName();

    private static final String MSG_TYPE = "msgType";
    private static final String MSG = "msg";

    private JsonHelper() {
    }

    public static String objectToJsonString(final Object obj) {
        JSONObject o = new JSONObject();
        try {
            if (obj instanceof ClientConnectionQuitted) {
                o.put(MSG_TYPE, QUIT_CLIENT_MSG);
            } else if (obj instanceof BluetoothCommunicatorPair){
                o.put(MSG_TYPE, TARGET_PAIR_MSG);
            } else if (obj instanceof BluetoothCommunicatorString) {
                o.put(MSG_TYPE, STRING_MSG);
            } else if (obj instanceof BluetoothCommunicatorEvent) {
                o.put(MSG_TYPE, EVENT_MSG);
            } else if (obj instanceof BluetoothCommunicatorSubscriber) {
                o.put(MSG_TYPE, SUBSCRIBE_MSG);
            } else if (obj instanceof BluetoothCommunicatorTemplate) {
                o.put(MSG_TYPE, TUPLE_TEMPLATE_MSG);
            } else if (obj instanceof BluetoothCommunicatorTuple) {
                o.put(MSG_TYPE, TUPLE_MSG);
            } else if (obj instanceof BluetoothAck) {
                o.put(MSG_TYPE, BT_ACK);
            }
            String jsonObjString = new Gson().toJson(obj);
            o.put(MSG, jsonObjString);
        } catch (JSONException e) {
            Log.d(TAG, "objectToJsonString - JSONException " +e.getMessage());
        }
        Log.d(TAG, "objectToJsonString: " + o.toString());
        Log.d(TAG, "objectToJsonString: time " +(System.currentTimeMillis()));
        return o.toString();
    }

    public static void jsonToObject(final String jsonString) {
        try {
            JSONObject parsedObj = new JSONObject(jsonString);
            String sMsgType = parsedObj.getString(MSG_TYPE);
            String sMsgObj = parsedObj.getString(MSG);
            MsgType type = MsgType.valueOf(sMsgType);
            Log.i(TAG, "jsonToObject: " + sMsgObj);
            switch (type) {
                case BT_ACK:
                    EventBus.getDefault().post(new BluetoothAck());
                    break;
                case QUIT_CLIENT_MSG:
                    Log.d(TAG, "jsonToObject: quit client connection");
                    ClientConnectionQuitted qCliConn = new Gson().fromJson(sMsgObj, ClientConnectionQuitted.class);
                    Log.d(TAG, "jsonToObject: " + (System.currentTimeMillis()));
                    EventBus.getDefault().post(qCliConn);
                    break;
                case EVENT_MSG:
                    Log.d(TAG, "jsonToObject: eventMsg");
                    BluetoothCommunicatorEvent btCommEv = new Gson().fromJson(sMsgObj, BluetoothCommunicatorEvent.class);
                    Log.d(TAG, "jsonToObject: btCommEv properties - " + btCommEv.getEventType());
                    Log.d(TAG, "jsonToObject: " + (System.currentTimeMillis()));
                    EventBus.getDefault().post(btCommEv);
                    break;
                case TARGET_PAIR_MSG:
                    Log.d(TAG, "jsonToObject: BluetoothCommunicatorPair");
                    BluetoothCommunicatorPair btCommPair = new Gson().fromJson(sMsgObj, BluetoothCommunicatorPair.class);
                    Log.d(TAG, "jsonToObject: btCommPair properties - " + btCommPair.getMsgObj());
                    Log.d(TAG, "jsonToObject: " + (System.currentTimeMillis()));
                    EventBus.getDefault().post(btCommPair);
                    break;
                case STRING_MSG:
                    Log.d(TAG, "jsonToObject: BluetoothCommunicatorString");
                    BluetoothCommunicatorString btCommStr = new Gson().fromJson(sMsgObj, BluetoothCommunicatorString.class);
                    Log.d(TAG, "jsonToObject: btCommStr properties - " + btCommStr.getContent());
                    Log.d(TAG, "jsonToObject: " + (System.currentTimeMillis()));
                    EventBus.getDefault().post(btCommStr);
                    break;
                case SUBSCRIBE_MSG:
                    Log.d(TAG, "jsonToObject: Subscribe msg");
                    BluetoothCommunicatorSubscriber btCommSub = new Gson().fromJson(sMsgObj, BluetoothCommunicatorSubscriber.class);
                    Log.d(TAG, "jsonToObject: btCommSub properties - " + btCommSub.getAddress() + " - " + btCommSub.getEventType() + " - " + btCommSub.isSubscribe());
                    Log.d(TAG, "jsonToObject: " + (System.currentTimeMillis()));
                    EventBus.getDefault().post(btCommSub);
                    break;
                case TUPLE_MSG:
                    Log.d(TAG, "jsonToObject: tuple out msg");
                    BluetoothCommunicatorTuple btCommTup = new Gson().fromJson(sMsgObj, BluetoothCommunicatorTuple.class);
                    Log.d(TAG, "jsonToObject: btCommTup properties - " + btCommTup.getSenderAddress() + " - " + btCommTup.getMsgObj() + " - " + btCommTup.getTemplate());
                    Log.d(TAG, "jsonToObject: " + (System.currentTimeMillis()));
                    EventBus.getDefault().post(btCommTup);
                    break;
                case TUPLE_TEMPLATE_MSG:
                    Log.d(TAG, "jsonToObject: template msg");
                    BluetoothCommunicatorTemplate btCommTem = new Gson().fromJson(sMsgObj, BluetoothCommunicatorTemplate.class);
                    Log.d(TAG, "jsonToObject: btCommTup properties - " + btCommTem.getAddress() + " - " + btCommTem.getTemplate() + " - " + btCommTem.isToDelete());
                    Log.d(TAG, "jsonToObject: " + (System.currentTimeMillis()));
                    EventBus.getDefault().post(btCommTem);
                    break;
                default:
                    Log.d(TAG, "jsonToObject: default");
            }
        } catch (ClassCastException e) {
            Log.d(TAG, "jsonToObject - ClassCastException " +e.getMessage());
        } catch (JSONException e) {
            Log.d(TAG, "jsonToObject - JSONException " + e.getMessage());
        }
    }
}
