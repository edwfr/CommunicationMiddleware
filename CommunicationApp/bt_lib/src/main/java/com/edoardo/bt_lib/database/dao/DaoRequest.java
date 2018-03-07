package com.edoardo.bt_lib.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.edoardo.bt_lib.database.model.Request;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface DaoRequest {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertRequest(Request request);

    @Query("SELECT * FROM Request WHERE templateId=:templateId")
    List<Request> getAllRequest(final int templateId);

    @Query("SELECT * FROM Request WHERE templateId=:templateId AND isToDelete=0")
    List<Request> getAllReadRequest(final int templateId);

    @Query("SELECT * FROM Request WHERE templateId=:templateId AND isToDelete=1 LIMIT 1")
    Request getInRequest(final int templateId);

    @Query("DELETE FROM Request WHERE uid=:id")
    void deleteRequest(final int id);

}
