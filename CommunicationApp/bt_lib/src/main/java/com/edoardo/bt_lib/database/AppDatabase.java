package com.edoardo.bt_lib.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.edoardo.bt_lib.database.dao.DaoMsg;
import com.edoardo.bt_lib.database.dao.DaoMsgDispatcher;
import com.edoardo.bt_lib.database.dao.DaoRoutingTable;
import com.edoardo.bt_lib.database.dao.DaoSubscription;
import com.edoardo.bt_lib.database.dao.DaoRequest;
import com.edoardo.bt_lib.database.dao.DaoTupleSpace;
import com.edoardo.bt_lib.database.model.AckRow;
import com.edoardo.bt_lib.database.model.Msg;
import com.edoardo.bt_lib.database.model.MsgDispatcher;
import com.edoardo.bt_lib.database.model.RoutingTable;
import com.edoardo.bt_lib.database.model.Subscription;
import com.edoardo.bt_lib.database.model.Request;
import com.edoardo.bt_lib.database.model.TupleSpace;

import java.util.LinkedList;
import java.util.List;


@Database(entities = {AckRow.class, Msg.class, MsgDispatcher.class, Subscription.class, RoutingTable.class, TupleSpace.class, Request.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase database;

    public abstract DaoMsg daoMsg();
    public abstract DaoMsgDispatcher daoMsgDispatcher();
    public abstract DaoSubscription daoSubscription();
    public abstract DaoRoutingTable daoRoutingTable();
    public abstract DaoTupleSpace daoTupleSpace();
    public abstract DaoRequest daoRequest();


    public static AppDatabase getInMemoryDatabase(Context context) {
        if (database == null) {
            database =
                    Room.inMemoryDatabaseBuilder(context.getApplicationContext(), AppDatabase.class)
                            .allowMainThreadQueries()
                            .build();
        }
        return database;
    }

}