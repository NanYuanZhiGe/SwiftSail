package com.nyzg.swiftsail.fragment.report;

import android.annotation.SuppressLint;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.data.BarEntry;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.DateUtils;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dao.RecordTableBase;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailSleepDayFragment;
import com.nyzg.swiftsail.obj.Pair;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import kotlin.random.Random;

public class ReportDetailSleepFragment extends ReportDetailBaseFragment {
    @SuppressLint("DefaultLocale")
    final private static Function<Float, String> AVG_FORMATTER = avg -> {
        int hour = (int) (float) avg;
        int minute = (int) ((avg - hour) * 60f);
        return String.format("%d小时%d分钟", hour, minute);
    };

    final private static Function<Double, Float> POST_PROCESSOR = d -> (float) (d / 1000.0 / 3600.0);

    public static Fragment getInstance() {
        return new ReportDetailSleepFragment();
    }

    @Override
    protected Fragment getFirstPage() {
        return ReportDetailSleepDayFragment.getInstance(R.layout.fragment_report_detail_sleep_day);
    }

    @Override
    protected Function<Float, String> getAvgFormatter() {
        return AVG_FORMATTER;
    }

    @Override
    protected String getPageType() {
        return "Sleep";
    }

    @Override
    protected int getThemeColor() {
        return R.color.heavyPurple;
    }

    @Override
    protected RecordTableBase getRecordTable() {
        return SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordSleepTable();
    }

    @Override
    protected Function<Double, Float> getDataFormatter() {
        return POST_PROCESSOR;
    }

    @Override
    protected String getTitleText() {
        return "睡觉";
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
                entries.add(new BarEntry(entryCount, (sum / 1000f / 3600f / (float) count)));
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
                entries.add(new BarEntry(entryCount, (sum / 1000f / 3600f / (float) count)));
            }
        }
        return new Pair<>(startDay, entries);
    }

    /**
     * 生成从2026年1月1日到2026年1月4日的数据
     */
    private static Pair<Long, List<BarEntry>> getTestDayData() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 1, 4);
        long startDay = startDate.toEpochDay();
        long endDay = endDate.toEpochDay();
        List<BarEntry> result = new ArrayList<>();
        int count = 0;
        for (long i = startDay; i <= endDay; ++i) {
            int randomHour = Random.Default.nextInt(3, 9);
            int randomMinute = Random.Default.nextInt(0, 60);
            result.add(new BarEntry(count, (randomHour * 3600f + randomMinute * 60f) / 3600f));
            ++count;
        }
        return new Pair<>(startDay, result);
    }

    private static Pair<Long, List<BarEntry>> getTestMonthData() {
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        List<BarEntry> result = new ArrayList<>();
        int count = 0;
        for (long i = 0; i < 12; ++i) {
            int randomHour = Random.Default.nextInt(3, 9);
            int randomMinute = Random.Default.nextInt(0, 60);
            result.add(new BarEntry(count, (randomHour * 3600f + randomMinute * 60f) / 3600f));
            ++count;
        }
        return new Pair<>(startDate.toEpochDay(), result);
    }

    private static Pair<Long, List<BarEntry>> getTestYearData() {
        List<BarEntry> result = new ArrayList<>();
        result.add(new BarEntry(0, 6.5f));
        return new Pair<>(2025L, result);
    }
}