package com.nyzg.swiftsail.fragment.main;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.listener.RecordBtnListener;
import com.nyzg.swiftsail.viewmodel.RecordViewModel;

import java.util.ArrayList;
import java.util.List;

public class RecordFragment extends Fragment {
    private final static List<BarEntry> EMPTY_BARS = new ArrayList<>();

    static {
        for (int i = 0; i < 6; ++i) {
            EMPTY_BARS.add(new BarEntry(i, new float[]{0f, 0f}));
        }
    }

    private RecordViewModel recordViewModel;
    private BarChart barChart;
    private BarDataSet barDataSet;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.layout_record, container, false);
        father.findViewById(R.id.new_record).setOnTouchListener(new RecordBtnListener(requireContext()));
        this.barChart = father.findViewById(R.id.barChart);
        recordViewModel = new ViewModelProvider(this).get(RecordViewModel.class);
        initBarChart();
        recordViewModel.getMutableBarEntries().observe(
                getViewLifecycleOwner(), this::updateBarChart
        );
        recordViewModel.initBarChart();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            recordViewModel.updateBarChart(4, true, 10);
            recordViewModel.updateBarChart(3, false, 13);
            recordViewModel.updateBarChart(3, true, 200);
        }, 2000);
        return father;
    }

    private void initBarChart() {
        barDataSet = new BarDataSet(EMPTY_BARS, "运动时间段占比");
        //设置柱状图的样式
        barDataSet.setColors(
                ContextCompat.getColor(GlobalApplication.getAppContext(), R.color.linkBlue),
                ContextCompat.getColor(GlobalApplication.getAppContext(), R.color.linkOrange)
        );
        barDataSet.setStackLabels(new String[]{"跑步", "走路"}); // 图例标签
        barDataSet.setValueTextSize(12f);
        BarData barData = new BarData(barDataSet);
        barData.setDrawValues(false);//不要柱状图每个柱子得小字说明
        barChart.setData(barData);
        barData.setBarWidth(0.9f);
        //X 轴
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // 标签在底部
        xAxis.setDrawGridLines(false); // 不画竖向网格线
        xAxis.setDrawAxisLine(true);   // 保留X轴线
        xAxis.setGranularity(1f);      // 防止缩放时标签重复
        xAxis.setTextSize(12f);

        // 设置X 轴标签
        String[] timeLabels = new String[6];
        for (int i = 0; i < 6; i++) {
            timeLabels[i] = (i * 4) + ":00";
        }
        xAxis.setValueFormatter(new IndexAxisValueFormatter(timeLabels));

        //保留左边Y轴
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setDrawGridLines(false); // 关闭横向网格线
        leftAxis.setDrawAxisLine(true);
        leftAxis.setTextSize(12f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(1.1f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) (value * 100) + "%";
            }
        });
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false); //隐藏右边Y轴

        //关闭其他内容
        barChart.getDescription().setEnabled(false); // 隐藏描述
        barChart.getLegend().setEnabled(true);     // 显示图例
        barChart.setFitBars(true);                  // 防止柱子被裁剪
        barChart.setTouchEnabled(false);            // 禁用缩放/拖动
        barChart.invalidate();//刷新
    }

    private void updateBarChart(List<BarEntry> entries) {
        barDataSet.setValues(entries);
        barChart.notifyDataSetChanged();
        barChart.invalidate();
    }
}
