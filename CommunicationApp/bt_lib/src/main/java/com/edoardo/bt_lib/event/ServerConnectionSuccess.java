package com.edoardo.bt_lib.event;

public class ServerConnectionSuccess {

    private String clientAddress;

    public ServerConnectionSuccess(final String clientAddress) {
        this.clientAddress = clientAddress;
    }


    public String getClientAddress() {
        return clientAddress;
    }
}
