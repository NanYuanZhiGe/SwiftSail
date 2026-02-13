package com.nyzg.swiftsail.fragment.report.detail;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.DateUtils;
import com.nyzg.swiftsail.bean.MyJsonSerializer;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ThreadPool;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dbobj.Record;
import com.nyzg.swiftsail.obj.MySleep;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ReportDetailSleepDayFragment extends ReportDetailDayBaseFragment {

    public static Fragment getInstance(int layoutResource) {
        Fragment fragment = new ReportDetailSleepDayFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_KEY, layoutResource);
        fragment.setArguments(bundle);
        return fragment;
    }

    private PieChart pieChart;
    private RecyclerView detailTimeLine;
    private TextView timeInBed;
    private TextView totalSleep;
    private PieData pieData;
    private final MutableLiveData<Long> totalSleepData = new MutableLiveData<>(0L);
    private final MutableLiveData<Long>
            totalTimeInBed = new MutableLiveData<>(0L);
    private final MutableLiveData<List<Pair<Long, Long>>> sleepTimeLineList = new MutableLiveData<>(new ArrayList<>(0));
    private final MutableLiveData<long[]> sleepStageList = new MutableLiveData<>(null);

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void init(View father) {
        pieChart = father.findViewById(R.id.pieChart);
        pieChart.setCenterText("睡眠阶段");
        pieChart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.heavyPurple));
        sleepStageList.observe(getViewLifecycleOwner(), list -> {
            if (list == null) {
                pieChart.clear();
                return;
            }
            List<PieEntry> pieEntries = new ArrayList<>(4);
            pieEntries.addAll(List.of(
                    new PieEntry(0f, "清醒"),
                    new PieEntry(0f, "快速眼动"),
                    new PieEntry(0f, "浅睡眠"),
                    new PieEntry(0f, "深睡眠")
            ));
            for (int i = 0; i < list.length && i < 4; ++i) {
                float y = (float) (list[i] / 1000.0 / 3600.0);
                pieEntries.get(i).setY(y);
            }
            PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
            pieDataSet.setColors(List.of(
                    ContextCompat.getColor(requireContext(), R.color.sleepWake),
                    ContextCompat.getColor(requireContext(), R.color.sleepRem),
                    ContextCompat.getColor(requireContext(), R.color.sleepLight),
                    ContextCompat.getColor(requireContext(), R.color.sleepDeep)
            ));
            pieDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getPieLabel(float value, PieEntry pieEntry) {
                    int hour = (int) value;
                    int minute = (int) ((value - hour) * 60);
                    if (hour == 0) {
                        return String.format("%d分钟", minute);
                    }
                    return String.format("%d小时%02d分钟", hour, minute);
                }
            });
            pieDataSet.setValueTextSize(12f);
            pieDataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            if (pieData == null) {
                pieData = new PieData(pieDataSet);
                pieChart.setData(pieData);
                pieChart.setDescription(null);
                pieChart.setTouchEnabled(false);
            } else {
                pieData.setDataSet(pieDataSet);
            }
            pieChart.notifyDataSetChanged();
            pieChart.invalidate();
        });

        detailTimeLine = father.findViewById(R.id.detailTimeLine);
        detailTimeLine.setLayoutManager(new LinearLayoutManager(requireContext()));
        detailTimeLine.setAdapter(new MyRecyclerAdapter(sleepTimeLineList.getValue()));
        sleepTimeLineList.observe(getViewLifecycleOwner(), list -> {
            if (detailTimeLine.getAdapter() == null) {
                return;
            }
            MyRecyclerAdapter myRecyclerAdapter = (MyRecyclerAdapter) detailTimeLine.getAdapter();
            myRecyclerAdapter.resetData(list);
            myRecyclerAdapter.notifyDataSetChanged();
        });

        timeInBed = father.findViewById(R.id.timeInBed);
        totalSleep = father.findViewById(R.id.totalSleep);
        totalSleepData.observe(getViewLifecycleOwner(), l -> setTimeTextView(totalSleep, l / 1000.0 / 3600.0));
        totalTimeInBed.observe(getViewLifecycleOwner(), l -> setTimeTextView(timeInBed, l / 1000.0 / 3600.0));
    }

    @SuppressLint("DefaultLocale")
    private void setTimeTextView(@NonNull TextView textView, double time) {
        int hour = (int) time;
        int minute = (int) ((time - hour) * 60);
        String hourTime = String.format("%d", hour);
        SpannableString spannable = new SpannableString(String.format("%s小时%02d分钟", hourTime, minute));
        float smallSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, //sp
                getResources().getDisplayMetrics());
        int totalLen = spannable.length();
        spannable.setSpan(new AbsoluteSizeSpan((int) smallSizePx), hourTime.length(), hourTime.length() + 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new AbsoluteSizeSpan((int) smallSizePx), totalLen - 2, totalLen, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        textView.setText(spannable);
    }

    @Override
    protected boolean hasDateSelector() {
        return true;
    }


    /**
     * 查询某日的睡眠数据，并解析
     */
    @Override
    protected Consumer<LocalDate> getDateSelectorCallback() {
        return date -> CompletableFuture.supplyAsync(() -> {
            if (LoginRepository.getInstance().currentUser.getValue() == null) {
                return null;
            }
            try {
                RecordTable recordTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordTable();
                Record record = recordTable.getSpecificationRecord(LoginRepository.getInstance().currentUser.getValue().id, RecordType.SLEEP, date.toEpochDay());
                if (record == null || record.detailValue == null) {
                    clearAll();
                    return null;
                }
                MySleep mySleep = MyJsonSerializer.deSerialize(record.detailValue, MySleep.class);
                totalSleepData.postValue(mySleep.totalSleepTime);
                totalTimeInBed.postValue(mySleep.totalTimeInBed);
                long[] mainSleepStage = {mySleep.mainWakeTime, mySleep.mainRemTime, mySleep.mainLightTime, mySleep.mainDeepTime};
                sleepStageList.postValue(mainSleepStage);
                if (mySleep.sleepLines == null) {
                    sleepTimeLineList.postValue(Collections.emptyList());
                    return null;
                }
                List<Pair<Long, Long>> pairList = new ArrayList<>(mySleep.sleepLines.size());
                for (MySleep.SleepLine sleepLine : mySleep.sleepLines) {
                    pairList.add(new Pair<>(sleepLine.startEpoch, sleepLine.endEpoch));
                }
                sleepTimeLineList.postValue(pairList);
            } catch (Exception ignore) {
                clearAll();
            }
            return null;
        }, ThreadPool.QUICK_CHANGE_THREAD_POOL);
    }

    private void clearAll() {
        totalSleepData.postValue(0L);
        totalTimeInBed.postValue(0L);
        sleepTimeLineList.postValue(new ArrayList<>(0));
        sleepStageList.postValue(new long[4]);
    }

    private class MyRecyclerAdapter extends RecyclerView.Adapter<MyRecyclerAdapter.MyViewHolder> {
        private final List<Pair<Long, Long>> dataList;

        public MyRecyclerAdapter(@Nullable List<Pair<Long, Long>> dataList) {
            if (dataList == null) {
                this.dataList = new ArrayList<>(0);
            } else {
                this.dataList = new ArrayList<>(dataList.size());
                this.dataList.addAll(dataList);
            }
        }

        public void resetData(@Nullable List<Pair<Long, Long>> dataList) {
            this.dataList.clear();
            if (dataList != null) {
                this.dataList.addAll(dataList);
            }
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MyViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_report_sleep_day_record, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            TextView textView = holder.textView;
            Pair<Long, Long> pair = this.dataList.get(position);
            LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(pair.getA()), DateUtils.ZONE_ID);
            LocalDateTime endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(pair.getB()), DateUtils.ZONE_ID);
            String start1 = startTime.format(DateUtils.HH_mm);
            String start2 = endTime.format(DateUtils.HH_mm);
            String period;
            if (startTime.getHour() > 18 || startTime.getHour() < 4) {
                period = "晚上";
            } else if (startTime.getHour() > 15) {
                period = "下午";
            } else if (startTime.getHour() > 11) {
                period = "中午";
            } else {
                period = "早上";
            }
            SpannableString spannableString = new SpannableString(String.format("%s %s至%s", period, start1, start2));
            float smallSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, //sp
                    getResources().getDisplayMetrics());
            spannableString.setSpan(new AbsoluteSizeSpan((int) smallSizePx), 0, 2, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new AbsoluteSizeSpan((int) smallSizePx), 3 + start1.length(), 3 + start1.length() + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            textView.setText(spannableString);
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        public static class MyViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.txt);
            }
        }
    }
}