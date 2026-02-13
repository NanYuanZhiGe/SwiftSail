package com.nyzg.swiftsail.fragment.report;

import android.annotation.SuppressLint;

import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailSleepDayFragment;

import java.util.function.Function;


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
        return RecordType.SLEEP;
    }

    @Override
    protected int getThemeColor() {
        return R.color.heavyPurple;
    }

    @Override
    protected Function<Double, Float> getDataFormatter() {
        return POST_PROCESSOR;
    }

    @Override
    protected String getTitleText() {
        return "睡觉";
    }
}