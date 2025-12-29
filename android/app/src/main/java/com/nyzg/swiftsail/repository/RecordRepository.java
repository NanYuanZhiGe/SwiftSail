package com.nyzg.swiftsail.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.obj.RecordData;

public class RecordRepository {
    public final static RecordRepository INSTANCE = new RecordRepository();
    private final MutableLiveData<RecordData> mutableRecordData = new MutableLiveData<>();

    private RecordRepository() {
    }

    public void updateData(boolean useFeet, int startHour, int endHour, float meters, int durationSecond) {
        RecordData recordData;
        if (mutableRecordData.getValue() == null) {
            recordData = new RecordData();
        } else {
            recordData = new RecordData(mutableRecordData.getValue());
        }
        if (useFeet) {
            recordData.updateFeetData(startHour, endHour, meters, durationSecond);
        } else {
            recordData.updateWheelData(startHour, endHour, meters, durationSecond);
        }
        mutableRecordData.setValue(recordData);
    }

    public LiveData<RecordData> getLiveRecordData() {
        return mutableRecordData;
    }
}
