package com.edoardo.bt_lib.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.edoardo.bt_lib.database.model.TupleSpace;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface DaoTupleSpace {

    @Insert(onConflict = REPLACE)
    void insertTuple(TupleSpace tupleSpace);

    @Query("SELECT * FROM TupleSpace WHERE templateId=:templateId LIMIT 1")
    TupleSpace getTuple(final int templateId);

    @Query("SELECT * FROM TupleSpace WHERE templateId=:templateId")
    List<TupleSpace> getAllTuple(final int templateId);

    @Query("DELETE FROM TupleSpace WHERE uid=:id")
    void deleteTuple(final int id);

}
