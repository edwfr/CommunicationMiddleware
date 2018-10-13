package com.edoardo.bt_lib.msg;

import com.edoardo.bt_lib.enums.EventType;

public class BluetoothCommunicatorSubscriber {

    private EventType eventType;

    private boolean subscribe;

    private String address;

    public BluetoothCommunicatorSubscriber(final EventType eventType, final String address,
                                           final boolean subscribe){
        this.eventType = eventType;
        this.subscribe = subscribe;
        this.address = address;
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean isSubscribe() {
        return subscribe;
    }

    public String getAddress() {
        return address;
    }
}
