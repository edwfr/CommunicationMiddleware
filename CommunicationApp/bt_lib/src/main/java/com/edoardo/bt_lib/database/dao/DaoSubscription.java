package com.edoardo.bt_lib.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.edoardo.bt_lib.database.model.Subscription;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;


@Dao
public interface DaoSubscription {

    @Insert(onConflict = REPLACE)
    void insertSubscription(Subscription sub);

    @Query("SELECT * FROM Subscription WHERE mac = :mac ")
    List<Subscription> getAllSubForClient(final String mac);

    @Query("SELECT * FROM Subscription WHERE mac = :mac AND eventId=:evId LIMIT 1")
    Subscription isSubForClient(final int evId, final String mac);

    @Query("DELETE FROM Subscription WHERE uid= :id")
    void deleteSub(final int id);

}