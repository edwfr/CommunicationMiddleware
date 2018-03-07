package com.edoardo.bt_lib.database.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "TupleSpace")
public class TupleSpace {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uid")
    private int uid;
    @ColumnInfo(name = "templateId")
    private int templateId;
    @ColumnInfo(name = "mac")
    private String mac;
    @ColumnInfo(name = "msg")
    private String msg;

    public TupleSpace(){}

    @Ignore
    public TupleSpace(final int templateId, final String mac, final String msg){
        this.templateId = templateId;
        this.mac = mac;
        this.msg = msg;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getTemplateId() {
        return templateId;
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
