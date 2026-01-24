package com.nyzg.swiftsail.repository;

import android.location.Location;

import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.dbobj.RecordBackUp;

/**
 * 这个repo专门提供给RecordRecordFragment和RecordRecordService来使用
 * 用于更新当前的运动的记录
 */
public class RecordRecordRepository{
    public static RecordRecordRepository INSTANCE = new RecordRecordRepository();
    private final MutableLiveData<String> minuteSecond = new MutableLiveData<>();
    final private MutableLiveData<String> distanceKilo = new MutableLiveData<>();
    final private MutableLiveData<String> speedMeterSecond = new MutableLiveData<>();
    final private MutableLiveData<Location> location = new MutableLiveData<>();
    final private MutableLiveData<Long> sportStartTime = new MutableLiveData<>(System.currentTimeMillis());
    private final MutableLiveData<RecordBackUp> recordBackUp = new MutableLiveData<>();

    private RecordRecordRepository() {
    }

    public MutableLiveData<Long> getSportStartTime() {
        return sportStartTime;
    }

    public MutableLiveData<RecordBackUp> getRecordBackUp() {
        return recordBackUp;
    }

    public MutableLiveData<String> getMinuteSecond() {
        return minuteSecond;
    }

    public MutableLiveData<String> getDistanceKilo() {
        return distanceKilo;
    }

    public MutableLiveData<String> getSpeedMeterSecond() {
        return speedMeterSecond;
    }

    public MutableLiveData<Location> getLocation() {
        return location;
    }

    public void setMinuteSecond(String s) {
        this.minuteSecond.setValue(s);
    }

    public void setDistanceKilo(String s) {
        this.distanceKilo.setValue(s);
    }

    public void setSpeedMeterSecond(String s) {
        this.speedMeterSecond.setValue(s);
    }

    public void setLocation(Location l) {
        this.location.setValue(l);
    }

    public void initValue() {
        this.distanceKilo.setValue("0");
        this.minuteSecond.setValue("00:00");
        this.speedMeterSecond.setValue("--");
    }
}