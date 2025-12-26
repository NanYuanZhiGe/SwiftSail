package com.nyzg.swiftsail.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class RecordViewModel extends ViewModel {
    private final MutableLiveData<List<BarEntry>> mutableBarEntries = new MutableLiveData<>();
    private final MutableLiveData<Boolean> haveInit = new MutableLiveData<>(false);

    private final float[] runBars = new float[6];
    private final float[] walkBars = new float[6];
    private float runMeters = 0f;
    private float walkMeters = 0f;

    public MutableLiveData<List<BarEntry>> getMutableBarEntries() {
        return mutableBarEntries;
    }

    public void updateBarChart(int timePeriod, boolean isRun, float meters) {
        if (timePeriod < 0 || timePeriod > 5 || meters <= 0) {
            return;
        }
        if (isRun) {
            this.runMeters += meters;
            this.runBars[timePeriod] += meters;
        } else {
            this.walkMeters += meters;
            this.walkBars[timePeriod] += meters;
        }
        mutableBarEntries.postValue(generateBarEntryList());
    }

    public void initBarChart() {
        if (Boolean.FALSE.equals(haveInit.getValue())) {
            mutableBarEntries.setValue(generateBarEntryList());
            haveInit.setValue(true);
        }
    }

    private List<BarEntry> generateBarEntryList() {
        List<BarEntry> barEntries = new ArrayList<>(6);
        float totalMeters = runMeters + walkMeters;
        for (int i = 0; i < 6; ++i) {
            if (Math.abs(totalMeters) < 1e-1) {
                barEntries.add(new BarEntry(i, new float[]{0f, 0f}));
                continue;
            }
            barEntries.add(new BarEntry(i, new float[]{runBars[i] / totalMeters, walkBars[i] / totalMeters}));
        }
        return barEntries;
    }
}
