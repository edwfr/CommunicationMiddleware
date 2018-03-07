package com.edoardo.bt_lib.event;

public class ClientConnectionQuitted {

    private String clientAddress;

    public ClientConnectionQuitted(String clientAddress) {
        this.clientAddress = clientAddress;
    }

    public String getClientAddress() {
        return clientAddress;
    }


}
