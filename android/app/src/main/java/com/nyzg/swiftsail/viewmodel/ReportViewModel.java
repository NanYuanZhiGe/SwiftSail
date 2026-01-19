package com.nyzg.swiftsail.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nyzg.swiftsail.dbobj.Report;
import com.nyzg.swiftsail.repository.ReportRepository;

public class ReportViewModel extends ViewModel {
    private final MutableLiveData<Report> mutableReport = new MutableLiveData<>();

    public MutableLiveData<Report> getMutableReport() {
        return mutableReport;
    }

    public void loadDataAsync(Long day) {
        ReportRepository.INSTANCE.getDataAsync(day).thenAccept(mutableReport::postValue);
    }
}