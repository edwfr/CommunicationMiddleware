package com.edoardo.bt_lib.event;

public class ClientConnectionFail {

    public final String serverAddress;

    public ClientConnectionFail(String serverAddress) {
        this.serverAddress = serverAddress;
    }
}
