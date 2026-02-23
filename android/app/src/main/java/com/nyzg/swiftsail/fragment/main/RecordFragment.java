package com.nyzg.swiftsail.fragment.main;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.repository.RecordRepository;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class RecordFragment extends MainBase {
    private final static List<BarEntry> EMPTY_BARS = new ArrayList<>();

    static {
        for (int i = 0; i < 6; ++i) {
            EMPTY_BARS.add(new BarEntry(i, new float[]{0f, 0f}));
        }
    }

    final private RecordRepository recordRepository = RecordRepository.getInstance();
    private BarChart barChart;
    private BarDataSet barDataSet;
    private final static DecimalFormat DISTANCE_FORMATTER = new DecimalFormat("0.00");
    private boolean hasInitBar = false;

    @SuppressLint("DefaultLocale")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.layout_record, container, false);
        /*
        father.findViewById(R.id.new_record).setOnTouchListener(new RecordBtnListener(requireContext()));
        this.barChart = father.findViewById(R.id.barChart);
        initBarChart();
        //需要进行UI更新的组件
        TextView numberDistance = father.findViewById(R.id.numberDistance);
        TextView numberStep = father.findViewById(R.id.numberStep);
        TextView numberKalo = father.findViewById(R.id.numberKa);

         */

        return father;
    }

    private void initBarChart() {
        if (hasInitBar) {
            return;
        }
        hasInitBar = true;
        barDataSet = new BarDataSet(EMPTY_BARS, "运动时间段占比");
        //设置柱状图的样式
        barDataSet.setColors(
                ContextCompat.getColor(GlobalApplication.getAppContext(), R.color.linkBlue),
                ContextCompat.getColor(GlobalApplication.getAppContext(), R.color.linkOrange)
        );
        barDataSet.setStackLabels(new String[]{"走/跑", "骑行"}); // 图例标签
        barDataSet.setValueTextSize(12f);

        BarData barData = new BarData(barDataSet);
        barData.setDrawValues(false);//不要柱状图每个柱子得小字说明
        barChart.setData(barData);
        barData.setBarWidth(0.25f);


        //X 轴
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // 标签在底部
        xAxis.setDrawGridLines(false); // 不画竖向网格线
        xAxis.setDrawAxisLine(false);   // 保留X轴线
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
        leftAxis.setDrawAxisLine(false);
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