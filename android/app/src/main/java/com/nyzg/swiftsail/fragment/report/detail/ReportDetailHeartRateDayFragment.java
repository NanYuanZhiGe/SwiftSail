package com.nyzg.swiftsail.fragment.report.detail;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ThreadPool;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dbobj.Record;
import com.nyzg.swiftsail.obj.MyHeartRate;
import com.nyzg.swiftsail.obj.MyStep;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ReportDetailHeartRateDayFragment extends ReportDetailDayBaseFragment {
    public static Fragment getInstance(int layoutResource) {
        Fragment fragment = new ReportDetailHeartRateDayFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_KEY, layoutResource);
        fragment.setArguments(bundle);
        return fragment;
    }


    private final MutableLiveData<MyHeartRate> heartRateData = new MutableLiveData<>();
    private TextView restHeartRate;
    private TextView outOfRangeCal;
    private TextView fatBurnCal;
    private TextView cardioCal;
    private TextView peakCal;

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    protected void init(View father) {
        restHeartRate = father.findViewById(R.id.restHeartRate);
        outOfRangeCal = father.findViewById(R.id.t23);
        fatBurnCal = father.findViewById(R.id.t33);
        cardioCal = father.findViewById(R.id.t43);
        peakCal = father.findViewById(R.id.t53);
        heartRateData.observe(getViewLifecycleOwner(), data -> {
            if (data == null) {
                return;
            }
            restHeartRate.setText(data.rest + "");
            float[] caloriesOut;
            if (data.caloriesOut == null) {
                caloriesOut = new float[4];
            } else if (data.caloriesOut.length < 4) {
                caloriesOut = new float[4];
                System.arraycopy(data.caloriesOut, 0, caloriesOut, 0, data.caloriesOut.length);
            } else {
                caloriesOut = data.caloriesOut;
            }
            outOfRangeCal.setText(String.format("%.2f", caloriesOut[0]));
            fatBurnCal.setText(String.format("%.2f", caloriesOut[1]));
            cardioCal.setText(String.format("%.2f", caloriesOut[2]));
            peakCal.setText(String.format("%.2f", caloriesOut[3]));
        });
    }

    @Override
    protected boolean hasDateSelector() {
        return true;
    }

    @Override
    protected Consumer<LocalDate> getDateSelectorCallback() {
        return localDate -> CompletableFuture.supplyAsync(() -> {
            if (LoginRepository.getInstance().currentUser.getValue() == null) {
                reset();
                return null;
            }
            try {
                RecordTable recordTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordTable();
                Record record = recordTable.getSpecificationRecord(LoginRepository.getInstance().currentUser.getValue().id,
                        RecordType.HEART, localDate.toEpochDay());
                if (record == null || record.detailValue == null) {
                    reset();
                    return null;
                }
                MyHeartRate myHeartRate = MyJsonSerializer.deSerialize(record.detailValue, MyHeartRate.class);
                heartRateData.postValue(myHeartRate);
            } catch (Exception ignore) {
                reset();
            }
            return null;
        }, ThreadPool.QUICK_CHANGE_THREAD_POOL);
    }

    private void reset() {
        heartRateData.postValue(new MyHeartRate());
    }
}