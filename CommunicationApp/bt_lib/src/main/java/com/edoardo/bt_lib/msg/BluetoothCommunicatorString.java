package com.edoardo.bt_lib.msg;

public class BluetoothCommunicatorString {

    private String content;

    public BluetoothCommunicatorString(final String message){
        this.content = message;
    }

    public String getContent() {
        return content;
    }

}
