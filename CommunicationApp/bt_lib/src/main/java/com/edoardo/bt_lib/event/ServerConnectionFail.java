package com.edoardo.bt_lib.event;

public class ServerConnectionFail {

    private String clientAddress;

    public ServerConnectionFail(final String mClientAddress) {
        clientAddress = mClientAddress;
    }

    public String getClientAddress() {
        return clientAddress;
    }
}
