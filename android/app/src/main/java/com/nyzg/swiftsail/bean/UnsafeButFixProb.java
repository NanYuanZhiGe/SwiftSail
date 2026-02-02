package com.nyzg.swiftsail.bean;

import androidx.lifecycle.MutableLiveData;

public class UnsafeButFixProb {
    final public static MutableLiveData<Integer> innerFragmentHeight = new MutableLiveData<>(-1);

    public static int getInnerHeight() {
        assert innerFragmentHeight.getValue() != null;
        return innerFragmentHeight.getValue();
    }
}
