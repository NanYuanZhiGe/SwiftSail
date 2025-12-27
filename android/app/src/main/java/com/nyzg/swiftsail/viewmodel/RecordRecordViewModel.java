package com.nyzg.swiftsail.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RecordRecordViewModel extends ViewModel {
    private final MutableLiveData<String> timeMinuteSecond = new MutableLiveData<>();
    private final MutableLiveData<String> distanceKiloMeter = new MutableLiveData<>();
    private final MutableLiveData<String> speedMeterSecond = new MutableLiveData<>();

    private long startTime;
    private float currentKiloMeter;

    public MutableLiveData<String> getTimeMinuteSecond() {
        return timeMinuteSecond;
    }

    public MutableLiveData<String> getDistanceKiloMeter() {
        return distanceKiloMeter;
    }

    public MutableLiveData<String> getSpeedMeterSecond() {
        return speedMeterSecond;
    }

    public void setTimeMinuteSecond(String s) {
        this.timeMinuteSecond.setValue(s);
    }

    public void setDistanceKiloMeter(String s) {
        this.distanceKiloMeter.setValue(s);
    }

    public void setSpeedMeterSecond(String s) {
        this.speedMeterSecond.setValue(s);
    }

    public void initDefaultValue() {
        timeMinuteSecond.setValue("00:00");
        distanceKiloMeter.setValue("0");
        speedMeterSecond.setValue("--");
        startTime = 0;
        currentKiloMeter = 0;
    }
}
