package com.edoardo.bt_lib.database.model;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.PrimaryKey;

@Entity(tableName = "Request")
public class Request {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uid")
    private int uid;
    @ColumnInfo(name = "templateId")
    private int templateId;
    @ColumnInfo(name = "mac")
    private String mac;
    @ColumnInfo(name = "isToDelete")
    private int isToDelete;

    public Request(){}

    @Ignore
    public Request(final int templateId, final String mac, final int isToDelete){
        this.templateId = templateId;
        this.mac = mac;
        this.isToDelete = isToDelete;
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

    public int getIsToDelete() {
        return isToDelete;
    }

    public void setIsToDelete(int isToDelete) {
        this.isToDelete = isToDelete;
    }
}
