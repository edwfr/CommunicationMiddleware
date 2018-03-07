package com.edoardo.bt_lib.msg;

import android.support.annotation.NonNull;

import com.edoardo.bt_lib.enums.Template;

public class BluetoothCommunicatorTemplate implements Comparable{

    private Template template;

    private boolean delete;

    private String address;

    public BluetoothCommunicatorTemplate(final Template template, final String address,
                                         final boolean delete){
        this.template = template;
        this.delete = delete;
        this.address = address;
    }

    public BluetoothCommunicatorTemplate(){}


    public Template getTemplate() {
        return template;
    }

    public boolean isToDelete() {
        return delete;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        return 0;
    }
}
