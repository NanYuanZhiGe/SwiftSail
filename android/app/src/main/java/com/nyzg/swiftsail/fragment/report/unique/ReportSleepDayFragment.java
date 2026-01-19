package com.nyzg.swiftsail.fragment.report.unique;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.fragment.report.ReportDetailBase;
import com.nyzg.swiftsail.view.RoundedBarChart;

import java.util.ArrayList;
import java.util.List;

public class ReportSleepDayFragment extends ReportDetailBase {
    public static Fragment getInstance(int layoutResource) {
        Fragment fragment = new ReportSleepDayFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_KEY, layoutResource);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected void init(View father) {
        RecyclerView recyclerView = father.findViewById(R.id.allSleepRecord);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<String> testList = new ArrayList<>(2);
        testList.add("上午12:18至10:48");
        testList.add("中午13:08至13:31");
        recyclerView.setAdapter(new MyRecyclerAdapter(testList));
        RoundedBarChart roundedBarChart = father.findViewById(R.id.sleepRange);
        initBarChart(roundedBarChart);
    }

    private void initBarChart(RoundedBarChart barChart) {
        // 准备数据
        List<BarEntry> barEntries = new ArrayList<>();
        List<BarEntry> barEntries1 = new ArrayList<>();
        List<BarEntry> barEntries2 = new ArrayList<>();
        List<BarEntry> barEntries3 = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            barEntries.add(new BarEntry(i, 0.1f));
            barEntries1.add(new BarEntry(i, 0.3f));
            barEntries2.add(new BarEntry(i, 0.2f));
            barEntries3.add(new BarEntry(i, 0.25f));
        }

        BarDataSet barDataSet = new BarDataSet(barEntries, "清醒");
        barDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.sleepWake));

        BarDataSet barDataSet1 = new BarDataSet(barEntries1, "快速眼动");
        barDataSet1.setColor(ContextCompat.getColor(requireContext(), R.color.sleepRem));

        BarDataSet barDataSet2 = new BarDataSet(barEntries2, "浅睡眠");
        barDataSet2.setColor(ContextCompat.getColor(requireContext(), R.color.sleepLight));

        BarDataSet barDataSet3 = new BarDataSet(barEntries3, "深度睡眠");
        barDataSet3.setColor(ContextCompat.getColor(requireContext(), R.color.sleepDeep));

        // 配置参数
        float barWidth = 0.2f;
        float barSpace = 0.03f;
        float groupSpace = 0.15f;
        int groupCount = 5;
        int dataSetCount = 4;

        BarData barData = new BarData(barDataSet, barDataSet1, barDataSet2, barDataSet3);
        barData.setBarWidth(barWidth);
        barData.setDrawValues(false);

        barData.groupBars(0, groupSpace, barSpace);
        barChart.setData(barData);

        // 计算 X 轴最大值（防止右边被裁）
        float groupWidth = barWidth * dataSetCount + barSpace * (dataSetCount - 1);
        float axisMax = (groupWidth + groupSpace) * groupCount - groupSpace + barWidth + barSpace;

        // 配置 X 轴
        XAxis xAxis = barChart.getXAxis();
        String[] timeLabels = {"1", "2", "3", "4", "5"};
        xAxis.setValueFormatter(new IndexAxisValueFormatter(timeLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(axisMax);

        // Y 轴配置
        barChart.getAxisLeft().setEnabled(false);
        barChart.getAxisRight().setAxisMinimum(0f);
        barChart.getAxisRight().setDrawGridLines(false);
        barChart.getAxisRight().setDrawAxisLine(false);

        // 图表行为
        barChart.setDescription(null);
        barChart.setTouchEnabled(false);

        barChart.setFitBars(true);
        barChart.setExtraOffsets(0, 0, 0, 0); // 消除默认 padding

        barChart.invalidate();
    }

    private static class MyRecyclerAdapter extends RecyclerView.Adapter<MyRecyclerAdapter.MyViewHolder> {
        private final List<String> dataList;

        public MyRecyclerAdapter(List<String> dataList) {
            this.dataList = dataList;
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
            ((TextView) holder.itemView).setText(dataList.get(position));
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        public static class MyViewHolder extends RecyclerView.ViewHolder {
            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                TextView textView = (TextView) itemView;
                textView.setTextSize(24f);
            }
        }
    }
}