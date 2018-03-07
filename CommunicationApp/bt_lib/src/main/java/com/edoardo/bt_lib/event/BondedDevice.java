package com.edoardo.bt_lib.event;

public class BondedDevice {

    private final String macAddress;

    public BondedDevice(final String address) {
        this.macAddress = address;
    }

    public String getMacAddress() {
        return this.macAddress;
    }
}
