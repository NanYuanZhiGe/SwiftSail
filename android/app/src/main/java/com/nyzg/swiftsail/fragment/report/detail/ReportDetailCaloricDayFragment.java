package com.nyzg.swiftsail.fragment.report.detail;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ThreadPool;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dbobj.Record;
import com.nyzg.swiftsail.obj.MyConsumption;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.view.RoundedBarChart;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ReportDetailCaloricDayFragment extends ReportDetailDayBaseFragment {
    public static Fragment getInstance(int layoutResource) {
        Fragment fragment = new ReportDetailCaloricDayFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_KEY, layoutResource);
        fragment.setArguments(bundle);
        return fragment;
    }

    private final MutableLiveData<MyConsumption> liveData = new MutableLiveData<>();

    private TextView totalCaloric;
    private TextView activityCaloric;
    private TextView activityPercentage;
    private View activityView;
    private RoundedBarChart barChart;
    private BarData barData;

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    protected void init(View father) {
        totalCaloric = father.findViewById(R.id.totalCaloric);
        activityCaloric = father.findViewById(R.id.activityCaloric);
        activityPercentage = father.findViewById(R.id.activityPercentage);
        activityView = father.findViewById(R.id.activityView);
        barChart = father.findViewById(R.id.pieChart);
        liveData.observe(getViewLifecycleOwner(), data -> {
            if (data == null) {
                return;
            }
            totalCaloric.setText(data.caloriesOut + "");
            activityCaloric.setText(data.activityCalories + "");
            if (data.caloriesOut <= 0) {
                activityPercentage.setText("0");
                ViewGroup.LayoutParams layoutParams = activityView.getLayoutParams();
                layoutParams.width = dpToPx(requireContext(), 10f);
                activityView.setLayoutParams(layoutParams);
            } else {
                float percent = (float) data.activityCalories / data.caloriesOut;
                activityPercentage.setText((int) (percent * 100) + "");
                ViewGroup.LayoutParams layoutParams = activityView.getLayoutParams();
                layoutParams.width = dpToPx(requireContext(), percent * 350f);
                activityView.setLayoutParams(layoutParams);
            }
            List<BarEntry> entries = new ArrayList<>(3);
            entries.add(new BarEntry(0f, (float) data.lightlyActiveMinutes));
            entries.add(new BarEntry(1f, (float) data.fairlyActiveMinutes));
            entries.add(new BarEntry(2f, (float) data.veryActiveMinutes));
            BarDataSet dataSet = new BarDataSet(entries, null);
            dataSet.setColors(ContextCompat.getColor(requireContext(), R.color.distanceModerate));
            if (barData == null) {
                barData = new BarData();
                barData.setDrawValues(false);
                barData.addDataSet(dataSet);
                barData.setBarWidth(0.2f);
                barChart.setDescription(null);
                barChart.setTouchEnabled(false);
                barChart.setData(barData);
                List<String> labels = List.of("轻度活动", "中等强度运动", "高强度运动");
                barChart.getXAxis().setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getAxisLabel(float value, AxisBase axis) {
                        if (value == 0f) {
                            return labels.get(0);
                        }
                        if (value == 1f) {
                            return labels.get(1);
                        }
                        if (value == 2f) {
                            return labels.get(2);
                        }
                        return "";
                    }
                });
                barChart.getXAxis().setGranularity(1f);
                barChart.getXAxis().setDrawAxisLine(false);
                barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                barChart.getXAxis().setDrawGridLines(false);
                barChart.getAxisLeft().setEnabled(false);
                barChart.setDrawGridBackground(false);
                barChart.getAxisRight().setDrawAxisLine(false);
                barChart.getAxisRight().setDrawGridLines(false);
                barChart.getAxisRight().setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getAxisLabel(float value, AxisBase axis) {
                        return String.format("%.0f 分钟", value);
                    }
                });
                barChart.setFitBars(true);
            } else {
                barData.clearValues();
                barData.addDataSet(dataSet);
            }
            barData.notifyDataChanged();
            barChart.invalidate();
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
                        RecordType.CALORIC, localDate.toEpochDay());
                if (record == null || record.detailValue == null) {
                    reset();
                    return null;
                }
                MyConsumption myCaloric = MyJsonSerializer.deSerialize(record.detailValue, MyConsumption.class);
                liveData.postValue(myCaloric);
            } catch (Exception ignore) {
                reset();
            }
            return null;
        }, ThreadPool.QUICK_CHANGE_THREAD_POOL);
    }

    private void reset() {
        liveData.postValue(new MyConsumption());
    }

    private int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }
}
