package com.edoardo.bt_lib.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.edoardo.bt_lib.bluetooth.BluetoothManager;

public class SharedPreferencesManager {

    private static final String APP_SETTINGS = "BT_LIB_SETTINGS";
    // properties
    private static final String BT_TYPE = "BT_TYPE";
    private static final String SERVER_ADDRESS = "SERVER_ADDRESS";

    private SharedPreferencesManager() {
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE);
    }

    public static String getBtTypeValue(Context context) {
        if (getSharedPreferences(context).contains(BT_TYPE)) {
            return getSharedPreferences(context).getString(BT_TYPE, "none");
        }
        return null;
    }

    public static String getServerAddressValue(Context context) {
        if (getSharedPreferences(context).contains(SERVER_ADDRESS)) {
            return getSharedPreferences(context).getString(SERVER_ADDRESS, null);
        }
        return null;
    }

    public static boolean containsType(Context context){
        return getSharedPreferences(context).contains(BT_TYPE);
    }

    public static void setBtType(Context context, BluetoothManager.TypeBluetooth t){
        final SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(BT_TYPE, t.toString());
        editor.apply();
    }

    public static void setServerAddress(Context context, String serverAddress){
        final SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(SERVER_ADDRESS, serverAddress);
        editor.apply();
    }

}