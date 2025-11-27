package com.nyzg.swiftsail.bean;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import com.nyzg.swiftsail.dao.UserTable;
import com.nyzg.swiftsail.dbobj.User;

@Database(entities = {
        User.class
}, version = 1, exportSchema = false)
public abstract class SQLiteDB extends RoomDatabase {
    private static SQLiteDB self;

    public abstract UserTable userTable();

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
            ).build();
        }
        return self;
    }

    @Override
    public void clearAllTables() {

    }

    @NonNull
    @Override
    protected InvalidationTracker createInvalidationTracker() {
        return null;
    }

    @NonNull
    @Override
    protected SupportSQLiteOpenHelper createOpenHelper(@NonNull DatabaseConfiguration databaseConfiguration) {
        return null;
    }
}
