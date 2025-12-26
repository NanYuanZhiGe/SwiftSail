package com.nyzg.swiftsail.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nyzg.swiftsail.fragment.record.RecordSelectFragment;

public class RecordSelectViewModel extends ViewModel {
    private final MutableLiveData<String> selectType = new MutableLiveData<>();

    public MutableLiveData<String> getSelectType() {
        return selectType;
    }

    public void changeFragment(String type) {
        if (type != null && RecordSelectFragment.TYPE_SET.contains(type) && !type.equals(getSelectType().getValue())) {
            selectType.setValue(type);
        }
    }

    public void setNull() {
        selectType.setValue(null);
    }
}
