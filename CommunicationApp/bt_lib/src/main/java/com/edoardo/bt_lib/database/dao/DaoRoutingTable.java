package com.edoardo.bt_lib.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.edoardo.bt_lib.database.model.RoutingTable;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface DaoRoutingTable {

    @Insert(onConflict = REPLACE)
    void insertRoute(RoutingTable routingTable);

    @Query("SELECT * FROM RoutingTable WHERE eventId=:evId")
    List<RoutingTable> getAllReceivers(final int evId);

    @Query("SELECT * FROM RoutingTable WHERE eventId=:evId AND mac = :mac LIMIT 1")
    RoutingTable isEventForReceiver(final int evId, final String mac);
    
    @Query("DELETE FROM RoutingTable WHERE uid=:id")
    void deleteRouteById(final int id);

}