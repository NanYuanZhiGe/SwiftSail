package com.nyzg.swiftsail.fragment.report;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.tabs.TabLayout;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.adapter.ReportDetailAdapter;
import com.nyzg.swiftsail.bean.DateUtils;
import com.nyzg.swiftsail.bean.UnsafeButFixProb;
import com.nyzg.swiftsail.fragment.report.unique.ReportSleepDayFragment;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.obj.SumType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import kotlin.random.Random;

public class ReportDetailSleepFragment extends Fragment {
    @SuppressLint("DefaultLocale")
    final private static Function<Float, String> AVG_FORMATTER = avg -> {
        int hour = (int) (float) avg;
        int minute = (int) ((avg - hour) * 60f);
        return String.format("%d小时%d分钟", hour, minute);
    };

    public static Fragment getInstance() {
        return new ReportDetailSleepFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_report_detail_sleep, container, false);
        TabLayout tabLayout = father.findViewById(R.id.summarySelector);
        tabLayout.addTab(tabLayout.newTab().setText("天"));
        tabLayout.addTab(tabLayout.newTab().setText("周"));
        tabLayout.addTab(tabLayout.newTab().setText("月"));
        tabLayout.addTab(tabLayout.newTab().setText("年"));
        tabLayout.addTab(tabLayout.newTab().setText("总"));
        father.findViewById(R.id.backspace).setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                .popBackStack());
        ViewPager2 viewPager2 = father.findViewById(R.id.summaries);
        viewPager2.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        viewPager2.setAdapter(new ReportDetailAdapter(
                position -> {
                    switch (position) {
                        case 0:
                            return ReportSleepDayFragment.getInstance(R.layout.fragment_report_detail_sleep_day);
                        case 1://week
                            return ReportDetailCommonFragment.getInstance(
                                    R.color.darkPurple,
                                    funcParam -> {
                                        return new Pair<>(0L, new ArrayList<>());
                                    },
                                    "ReportDetailSleepWeek",
                                    SumType.WEEK,
                                    AVG_FORMATTER
                            );
                        case 2://month
                            return ReportDetailCommonFragment.getInstance(
                                    R.color.darkPurple,
                                    funcParam -> {
                                        return getTestWeekData();
                                    },
                                    "ReportDetailSleepMonth",
                                    SumType.MONTH,
                                    AVG_FORMATTER
                            );
                        case 3://year
                            return ReportDetailCommonFragment.getInstance(
                                    R.color.darkPurple,
                                    funcParam -> {
                                        return new Pair<>(0L, new ArrayList<>());
                                    },
                                    "ReportDetailSleepYear",
                                    SumType.YEAR,
                                    AVG_FORMATTER
                            );
                        default://total
                            return ReportDetailCommonFragment.getInstance(
                                    R.color.darkPurple,
                                    funcParam -> {
                                        return new Pair<>(0L, new ArrayList<>());
                                    },
                                    "ReportDetailSleepTotal",
                                    SumType.TOTAL,
                                    AVG_FORMATTER
                            );
                    }
                },
                5,
                requireActivity()
        ));
        viewPager2.setUserInputEnabled(false);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                viewPager2.setCurrentItem(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        return father;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayout layout1 = view.findViewById(R.id.layout1);
        TabLayout tabLayout = view.findViewById(R.id.summarySelector);
        ViewPager2 viewPager2 = view.findViewById(R.id.summaries);
        view.post(() -> {
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int needHeight = UnsafeButFixProb.innerFragmentHeight;
            if (needHeight < 0) {
                needHeight = screenHeight;
            }
            // 获取 layout1 和 TabLayout 的实际高度
            int layout1Height = layout1.getHeight();
            int tabHeight = tabLayout.getHeight();
            // 计算 ViewPager2 应该有的高度
            int viewPagerHeight = needHeight - layout1Height - tabHeight;
            // 设置 ViewPager2 的 LayoutParams
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewPager2.getLayoutParams();
            params.height = Math.max(viewPagerHeight, 0); // 防止负数
            viewPager2.setLayoutParams(params);
        });
    }

    /**
     * 模拟从2025年2月1日到2025年2月28日的数据
     */
    private static Pair<Long, List<BarEntry>> getTestWeekData() {
        LocalDate startDate = LocalDate.of(2025, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 2, 28);
        long startDay = startDate.toEpochDay();
        long endDay = endDate.toEpochDay();
        List<BarEntry> entries = new ArrayList<>();
        long epochWeek = DateUtils.getEpochWeek(startDay);
        long sum = 0L;
        long count = 0;
        int entryCount = 0;
        for (long i = startDay; i <= endDay; ++i) {
            long tempEpoch = DateUtils.getEpochWeek(i);
            if (tempEpoch == epochWeek) {
                //模拟随机的睡眠时间
                int randomHour = Random.Default.nextInt(3, 9);
                int randomMinute = Random.Default.nextInt(0, 60);
                //毫秒
                sum += (randomHour * 3600L + randomMinute * 60L) * 1000L;
            } else {
                //出现新的一周
                //把上一周的数据加到BarEntry中
                entries.add(new BarEntry(entryCount, (float) (sum / 1000f / 3600f / (float) count)));
                epochWeek = tempEpoch;
                //还原数据
                ++entryCount;
                sum = 0L;
                count = 0L;
                //计算新的一周第一天的数据
                int randomHour = Random.Default.nextInt(3, 9);
                int randomMinute = Random.Default.nextInt(0, 60);
                sum += (randomHour * 3600L + randomMinute * 60L) * 1000L;
            }
            count += 1;
            if (i == endDay) {//最后一天了，不论如何，都要添加到BarEntry中
                entries.add(new BarEntry(entryCount, (float) (sum / 1000f / 3600f / (float) count)));
            }
        }
        return new Pair<>(startDay, entries);
    }
}