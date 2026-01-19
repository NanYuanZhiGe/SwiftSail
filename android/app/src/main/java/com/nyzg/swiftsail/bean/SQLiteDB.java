package com.nyzg.swiftsail.bean;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.nyzg.swiftsail.dao.LastLoginTable;
import com.nyzg.swiftsail.dao.RecordBackUpTable;
import com.nyzg.swiftsail.dao.UserTable;
import com.nyzg.swiftsail.dao.WatchTable;
import com.nyzg.swiftsail.dbobj.LastLogin;
import com.nyzg.swiftsail.dbobj.RecordBackUp;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.dbobj.Watch;

@Database(
        entities = {
                User.class,
                LastLogin.class,
                RecordBackUp.class,
                Watch.class
        },
        version = 7,
        exportSchema = false)
public abstract class SQLiteDB extends RoomDatabase {
    private volatile static SQLiteDB self;

    public abstract UserTable userTable();

    public abstract LastLoginTable lastLoginTable();

    public abstract RecordBackUpTable recordBackUpTable();
    public  abstract WatchTable watchTable();

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
