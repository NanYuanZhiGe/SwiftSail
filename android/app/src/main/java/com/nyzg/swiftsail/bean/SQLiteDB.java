package com.nyzg.swiftsail.bean;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.nyzg.swiftsail.dao.LastLoginTable;
import com.nyzg.swiftsail.dao.RecordManualTable;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dao.UserTable;
import com.nyzg.swiftsail.dbobj.LastLogin;
import com.nyzg.swiftsail.dbobj.Record;
import com.nyzg.swiftsail.dbobj.RecordManual;
import com.nyzg.swiftsail.dbobj.User;

@Database(
        entities = {
                User.class,
                LastLogin.class,
                RecordManual.class,
                //和fitbit的同步数据的类
                Record.class
        },
        version = 17,
        exportSchema = false)
public abstract class SQLiteDB extends RoomDatabase {
    private volatile static SQLiteDB self;

    public abstract UserTable userTable();

    public abstract LastLoginTable lastLoginTable();

    public abstract RecordManualTable recordManualTable();

    //fitbit同步数据库表
    public abstract RecordTable recordTable();

    protected SQLiteDB() {
    }

    static public SQLiteDB getDatabase(final Context context) {
        if (self != null) {
            return self;
        }
        synchronized (SQLiteDB.class) {
            if (self != null) {
                return self;
            }

            self = Room.databaseBuilder(
                    context.getApplicationContext(),
                    SQLiteDB.class,
                    "SwiftSailDB"
            ).fallbackToDestructiveMigration().build();
        }
        return self;
    }

    @Override
    public void clearAllTables() {

    }
}
