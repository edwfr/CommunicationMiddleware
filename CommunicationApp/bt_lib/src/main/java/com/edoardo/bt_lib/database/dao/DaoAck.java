package com.edoardo.bt_lib.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.edoardo.bt_lib.database.model.AckRow;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface DaoAck{

    @Insert(onConflict = REPLACE)
    Long insertAck(AckRow ackRow);

    @Query("SELECT * FROM AckRow")
    List<AckRow> getAllAck();

    @Query("SELECT * FROM AckRow WHERE msgId= :mId LIMIT 1")
    AckRow getAck(final int mId);

    @Query("DELETE FROM AckRow WHERE uid= :id")
    void deleteAck(final int id);

}
