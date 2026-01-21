package com.nyzg.swiftsail.bean;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.nyzg.swiftsail.dao.LastLoginTable;
import com.nyzg.swiftsail.dao.RecordBackUpTable;
import com.nyzg.swiftsail.dao.RecordConsumptionTable;
import com.nyzg.swiftsail.dao.RecordDistanceTable;
import com.nyzg.swiftsail.dao.RecordHeartRateTable;
import com.nyzg.swiftsail.dao.RecordNutritionTable;
import com.nyzg.swiftsail.dao.RecordSleepTable;
import com.nyzg.swiftsail.dao.RecordStepTable;
import com.nyzg.swiftsail.dao.UserTable;
import com.nyzg.swiftsail.dao.WatchTable;
import com.nyzg.swiftsail.dbobj.LastLogin;
import com.nyzg.swiftsail.dbobj.RecordBackUp;
import com.nyzg.swiftsail.dbobj.RecordConsumption;
import com.nyzg.swiftsail.dbobj.RecordDistance;
import com.nyzg.swiftsail.dbobj.RecordHeartRate;
import com.nyzg.swiftsail.dbobj.RecordNutrition;
import com.nyzg.swiftsail.dbobj.RecordSleep;
import com.nyzg.swiftsail.dbobj.RecordStep;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.dbobj.Watch;

@Database(
        entities = {
                User.class,
                LastLogin.class,
                RecordBackUp.class,
                Watch.class,
                //和fitbit的同步数据的类
                RecordConsumption.class,
                RecordDistance.class,
                RecordHeartRate.class,
                RecordNutrition.class,
                RecordSleep.class,
                RecordStep.class
        },
        version = 9,
        exportSchema = false)
public abstract class SQLiteDB extends RoomDatabase {
    private volatile static SQLiteDB self;

    public abstract UserTable userTable();

    public abstract LastLoginTable lastLoginTable();

    public abstract RecordBackUpTable recordBackUpTable();

    public abstract WatchTable watchTable();

    //fitbit同步数据库表
    public abstract RecordConsumptionTable recordConsumptionTable();

    public abstract RecordDistanceTable recordDistanceTable();

    public abstract RecordHeartRateTable recordHeartRateTable();

    public abstract RecordNutritionTable recordNutritionTable();

    public abstract RecordSleepTable recordSleepTable();

    public abstract RecordStepTable recordStepTable();

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
