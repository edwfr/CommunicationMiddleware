package com.edoardo.bt_lib.msg;

public class BluetoothCommunicatorPair {


    private String targetAddress;

    private BluetoothCommunicatorString msgObj;

    public BluetoothCommunicatorPair(final String targetAddress, final String message){
        this.targetAddress = targetAddress;
        this.msgObj = new BluetoothCommunicatorString(message);
    }

    public BluetoothCommunicatorString getMsgObj() {
        return msgObj;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

}
