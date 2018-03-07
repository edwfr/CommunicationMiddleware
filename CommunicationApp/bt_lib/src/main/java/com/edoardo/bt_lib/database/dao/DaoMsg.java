package com.edoardo.bt_lib.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.edoardo.bt_lib.database.model.Msg;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface DaoMsg {

    @Insert(onConflict = REPLACE)
    Long insertMsg(Msg msg);

    @Query("SELECT * FROM Msg")
    List<Msg> findAllMsg();

    @Query("SELECT * FROM Msg LIMIT 1")
    Msg getMsg();

    @Query("DELETE FROM Msg WHERE uid= :id")
    void deleteMsg(final int id);

}
