package com.nyzg.swiftsail.bean;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import com.nyzg.swiftsail.dao.LastLoginTable;
import com.nyzg.swiftsail.dao.UserTable;
import com.nyzg.swiftsail.dbobj.LastLogin;
import com.nyzg.swiftsail.dbobj.User;

@Database(entities = {
        User.class, LastLogin.class
}, version = 2, exportSchema = false)
public abstract class SQLiteDB extends RoomDatabase {
    private volatile static SQLiteDB self;

    public abstract UserTable userTable();

    public abstract LastLoginTable lastLoginTable();

    protected SQLiteDB() {}

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
