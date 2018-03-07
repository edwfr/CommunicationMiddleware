package com.edoardo.bt_lib.database.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

/**
 * Created by Edoardo on 17/02/18.
 */

@Entity(tableName = "AckRow")
public class AckRow {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uid")
    private int uid;

    @ColumnInfo(name = "msgId")
    private int msgId;

    @ColumnInfo(name = "outTime")
    private long outTime;

    @ColumnInfo(name = "confirmed")
    private boolean confirmed;

    public AckRow(final int msgId){
        this.msgId = msgId;
        this.outTime = System.currentTimeMillis();
        this.confirmed = false;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getMsgId() {
        return msgId;
    }

    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public long getOutTime() {
        return outTime;
    }

    public void setOutTime(long outTime) {
        this.outTime = outTime;
    }
}
