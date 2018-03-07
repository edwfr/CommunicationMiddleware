package com.edoardo.bt_lib.event;

public class ClientConnectionFail {

    public String serverAddress;

    public ClientConnectionFail(String serverAddress) {
        this.serverAddress = serverAddress;
    }
}
