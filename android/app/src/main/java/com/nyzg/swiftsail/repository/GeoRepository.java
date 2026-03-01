package com.nyzg.swiftsail.repository;

import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.dbobj.PersonalData;

public class GeoRepository {
    private static volatile GeoRepository self;

    private GeoRepository() {
        PersonalData data = personalData.getValue();
        assert data != null;
        data.appellation = "称呼";
        data.promiseScore = 10;
        data.inTimeScore = 10;
        data.levelScore = 10;
        data.cooperationScore = 10;
        data.communicateScore = 10;
    }

    final public MutableLiveData<PersonalData> personalData = new MutableLiveData<>(new PersonalData());

    static public GeoRepository getInstance() {
        if (self != null) {
            return self;
        }
        synchronized (GeoRepository.class) {
            if (self != null) {
                return self;
            }
            self = new GeoRepository();
            return self;
        }
    }
}
