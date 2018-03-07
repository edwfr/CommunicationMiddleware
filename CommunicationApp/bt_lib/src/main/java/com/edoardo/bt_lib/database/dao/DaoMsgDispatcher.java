package com.edoardo.bt_lib.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.edoardo.bt_lib.database.model.MsgDispatcher;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface DaoMsgDispatcher {

    @Insert(onConflict = REPLACE)
    void insertMsg(MsgDispatcher msg);

    @Query("SELECT * FROM MsgDispatcher WHERE mac = :mac ")
    List<MsgDispatcher> getAllMsgForClient(final String mac);

    @Query("SELECT * FROM MsgDispatcher WHERE mac = :mac LIMIT 1")
    MsgDispatcher getMsgForClient(final String mac);

    @Query("DELETE FROM MsgDispatcher WHERE uid= :id")
    void deleteMsg(final int id);

}
