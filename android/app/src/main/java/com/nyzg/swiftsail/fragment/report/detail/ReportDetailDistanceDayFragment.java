package com.nyzg.swiftsail.fragment.report.detail;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ThreadPool;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dbobj.Record;
import com.nyzg.swiftsail.obj.MyDistance;
import com.nyzg.swiftsail.obj.MyStep;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ReportDetailDistanceDayFragment extends ReportDetailDayBaseFragment {
    public static Fragment getInstance(int layoutResource) {
        Fragment fragment = new ReportDetailDistanceDayFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_KEY, layoutResource);
        fragment.setArguments(bundle);
        return fragment;
    }

    private final MutableLiveData<MyDistance> distanceData = new MutableLiveData<>();
    private TextView totalDistance;
    private TextView walkDistance;
    private TextView lightDistance;
    private TextView moderateDistance;
    private TextView activeDistance;
    private PieChart pieChart;
    private PieData pieData;

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    protected void init(View father) {
        totalDistance = father.findViewById(R.id.totalDistance);
        walkDistance = father.findViewById(R.id.walkDistance);
        lightDistance = father.findViewById(R.id.lightDistance);
        moderateDistance = father.findViewById(R.id.moderateDistance);
        activeDistance = father.findViewById(R.id.activeDistance);
        pieChart = father.findViewById(R.id.pieChart);
        distanceData.observe(getViewLifecycleOwner(), data -> {
            if (data == null) {
                return;
            }
            totalDistance.setText(String.format("%.2f", data.distance));
            walkDistance.setText(String.format("%.2f", data.walkDistance));
            lightDistance.setText(String.format("%.2f", data.lightlyActiveDistance));
            moderateDistance.setText(String.format("%.2f", data.moderateActiveDistance));
            activeDistance.setText(String.format("%.2f", data.veryActiveDistance));
            List<PieEntry> pieEntryList = new ArrayList<>(4);
            pieEntryList.add(new PieEntry((float) data.walkDistance, "步行"));
            pieEntryList.add(new PieEntry((float) data.lightlyActiveDistance, "轻度活动"));
            pieEntryList.add(new PieEntry((float) data.moderateActiveDistance, "中等强度运动"));
            pieEntryList.add(new PieEntry((float) data.veryActiveDistance, "高强度运动"));
            PieDataSet pieDataSet = new PieDataSet(pieEntryList, "");
            pieDataSet.setColors(List.of(
                    ContextCompat.getColor(requireContext(), R.color.distanceWalk),
                    ContextCompat.getColor(requireContext(), R.color.distanceLight),
                    ContextCompat.getColor(requireContext(), R.color.distanceModerate),
                    ContextCompat.getColor(requireContext(), R.color.distanceActive)
            ));
            pieDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getPieLabel(float value, PieEntry pieEntry) {
                    return String.format("%.2f 公里", value);
                }
            });
            pieDataSet.setValueTextSize(12f);
            pieDataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            if (pieData == null) {
                pieData = new PieData(pieDataSet);
                pieChart.setDescription(null);
                pieChart.setTouchEnabled(false);
                pieChart.setData(pieData);
                pieChart.setCenterText("活动距离");
            } else {
                pieData.setDataSet(pieDataSet);
            }
            pieData.notifyDataChanged();
            pieChart.invalidate();
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
                        RecordType.DISTANCE, localDate.toEpochDay());
                if (record == null || record.detailValue == null) {
                    reset();
                    return null;
                }
                MyDistance myDistance = MyJsonSerializer.deSerialize(record.detailValue, MyDistance.class);
                distanceData.postValue(myDistance);
            } catch (Exception ignore) {
                reset();
            }
            return null;
        }, ThreadPool.QUICK_CHANGE_THREAD_POOL);
    }

    private void reset() {
        distanceData.postValue(new MyDistance());
    }
}
