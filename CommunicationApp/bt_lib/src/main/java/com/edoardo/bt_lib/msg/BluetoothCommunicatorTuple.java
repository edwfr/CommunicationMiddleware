package com.edoardo.bt_lib.msg;

import android.support.annotation.NonNull;

import com.edoardo.bt_lib.enums.Template;
public class BluetoothCommunicatorTuple implements Comparable{

    private BluetoothCommunicatorString msgObj;

    private Template template;

    private String senderAddress;

    public BluetoothCommunicatorTuple(final String msgObj, final Template template, final String senderAddress){
        this.msgObj = new BluetoothCommunicatorString(msgObj);
        this.template = template;
        this.senderAddress = senderAddress;
    }

    public BluetoothCommunicatorTuple(final BluetoothCommunicatorString msgObj, final Template template, final String senderAddress){
        this.msgObj = msgObj;
        this.template = template;
        this.senderAddress = senderAddress;
    }

    public BluetoothCommunicatorString getMsgObj() {
        return msgObj;
    }

    public Template getTemplate() {
        return template;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        BluetoothCommunicatorTuple btCommTup = (BluetoothCommunicatorTuple)o;
        if (this.msgObj.getContent().length() >  btCommTup.getMsgObj().getContent().length()){
            return 0;
        }else {
            return 1;
        }
    }
}
