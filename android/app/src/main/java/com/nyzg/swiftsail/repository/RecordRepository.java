package com.nyzg.swiftsail.repository;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dbobj.RecordManual;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.obj.RecordData;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class RecordRepository {
    private static final Type TYPE_MAP = new TypeToken<Map<String, Object>>() {
    }.getType();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");
    private volatile static RecordRepository INSTANCE=null;

    //这个成员核心，它主要是用于RecordFragment的UI显示
    private final MutableLiveData<RecordData> recordData = new MutableLiveData<>();
    private final ReadWriteLock recordLock = new ReentrantReadWriteLock();
    private final AtomicBoolean OBSERVE_USER_LOCK = new AtomicBoolean(false);

    public static RecordRepository getInstance() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        synchronized (RecordRepository.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            INSTANCE = new RecordRepository();
        }
        return INSTANCE;
    }

    private RecordRepository() {
    }

    public void updateUserOnChange() {
        if (!OBSERVE_USER_LOCK.compareAndSet(false, true)) {
            return;
        }
        //由于LoginRepository的currentUser通过postValue的方式更新，所以执行的速度会比worker慢
        //导致user为空然后就没有办法更新用户数据，所以需要通过observe的方式进行数据更新
        //然后每次observe的触发，都必须进行全量更新，此时应该锁住所有的写操作
        LoginRepository.getInstance().getCurrentUser().observeForever(this::initUserRecordDataAsync);
    }

    /**
     * 根据当前用户重新加载一遍用户的运动记录
     *
     * @param user 当前用户
     */
    public void initUserRecordDataAsync(User user) {
        if (user == null) {
            return;
        }
    }

}
