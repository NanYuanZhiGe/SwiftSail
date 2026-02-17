package com.nyzg.swiftsail.repository;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.dbobj.RecordManual;
import com.nyzg.swiftsail.obj.Pair;

/**
 * 这个repo专门提供给RecordRecordFragment和RecordRecordService来使用
 * 用于更新当前的运动的记录
 */
public class RecordRecordRepository {
    final public MutableLiveData<String> SELECT_TYPE = new MutableLiveData<>();
    final public MutableLiveData<String> SELECT_NAME=new MutableLiveData<>();
    private static RecordRecordRepository self;

    public final MutableLiveData<Long> exposeRecord0 = new MutableLiveData<>(0L);
    public final MutableLiveData<String> detailRecord0 = new MutableLiveData<>();
    public final MutableLiveData<Location> locationGlobal = new MutableLiveData<>();
    final private MutableLiveData<Long> sportStartTime = new MutableLiveData<>(System.currentTimeMillis());
    private final MutableLiveData<RecordManual> recordBackUp = new MutableLiveData<>();

    static public @NonNull RecordRecordRepository getInstance() {
        if (self != null) {
            return self;
        }
        synchronized (RecordRecordRepository.class) {
            if (self != null) {
                return self;
            }
            self = new RecordRecordRepository();
        }
        return self;
    }

    private RecordRecordRepository() {
    }
}