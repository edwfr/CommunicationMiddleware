package com.edoardo.bt_lib.msg;

import com.edoardo.bt_lib.enums.EventType;

public class BluetoothCommunicatorEvent {

    private BluetoothCommunicatorString msgObj;

    private EventType eventType;

    private String senderAddress;

    public BluetoothCommunicatorEvent(final String msgObj, final EventType eventType, final String senderAddress){
        this.msgObj = new BluetoothCommunicatorString(msgObj);
        this.eventType = eventType;
        this.senderAddress = senderAddress;
    }

    public BluetoothCommunicatorEvent() {
    }

    public BluetoothCommunicatorString getMsgObj() {
        return msgObj;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

}
