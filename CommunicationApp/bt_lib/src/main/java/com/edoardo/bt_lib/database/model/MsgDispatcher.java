package com.edoardo.bt_lib.database.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "MsgDispatcher")
public class MsgDispatcher {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uid")
    private int uid;
    @ColumnInfo(name = "msg")
    private String msg;
    @ColumnInfo(name = "mac")
    private String mac;

    public MsgDispatcher(){}
    @Ignore
    public MsgDispatcher(final String msg, final String mac){
        this.msg = msg;
        this.mac = mac;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }
}
