package com.nyzg.swiftsail.repository;

import android.location.Location;

import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.dbobj.RecordBackUp;

/**
 * 这个repo专门提供给RecordRecordFragment和RecordRecordService来使用
 * 用于更新当前的运动的记录
 */
public class RecordRecordRepository {
    public static RecordRecordRepository INSTANCE = new RecordRecordRepository();
    private final MutableLiveData<String> mutableMinuteSecond = new MutableLiveData<>();
    final private MutableLiveData<String> mutableDistanceKilo = new MutableLiveData<>();
    final private MutableLiveData<String> mutableSpeedMeterSecond = new MutableLiveData<>();
    final private MutableLiveData<Location> mutableLocation = new MutableLiveData<>();
    final private MutableLiveData<Long> sportStartTime=new MutableLiveData<>(System.currentTimeMillis());
    private final MutableLiveData<RecordBackUp> recordBackUp=new MutableLiveData<>();

    private RecordRecordRepository() {
    }

    public MutableLiveData<Long> getSportStartTime() {
        return sportStartTime;
    }

    public MutableLiveData<RecordBackUp> getRecordBackUp() {
        return recordBackUp;
    }

    public MutableLiveData<String> getMutableMinuteSecond() {
        return mutableMinuteSecond;
    }

    public MutableLiveData<String> getMutableDistanceKilo() {
        return mutableDistanceKilo;
    }

    public MutableLiveData<String> getMutableSpeedMeterSecond() {
        return mutableSpeedMeterSecond;
    }

    public MutableLiveData<Location> getMutableLocation() {
        return mutableLocation;
    }

    public void setMinuteSecond(String s) {
        this.mutableMinuteSecond.setValue(s);
    }

    public void setDistanceKilo(String s) {
        this.mutableDistanceKilo.setValue(s);
    }

    public void setSpeedMeterSecond(String s) {
        this.mutableSpeedMeterSecond.setValue(s);
    }

    public void setLocation(Location l) {
        this.mutableLocation.setValue(l);
    }

    public void initValue() {
        this.mutableDistanceKilo.setValue("0");
        this.mutableMinuteSecond.setValue("00:00");
        this.mutableSpeedMeterSecond.setValue("--");
    }
}