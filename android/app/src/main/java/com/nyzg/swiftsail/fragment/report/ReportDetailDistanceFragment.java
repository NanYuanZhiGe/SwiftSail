package com.nyzg.swiftsail.fragment.report;

import android.annotation.SuppressLint;

import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailCommDayFragment;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailDistanceDayFragment;

import java.util.function.Function;

public class ReportDetailDistanceFragment extends ReportDetailBaseFragment {
    public static Fragment getInstance() {
        return new ReportDetailDistanceFragment();
    }

    @SuppressLint("DefaultLocale")
    private final static Function<Float, String> AVG_FORMATTER = aFloat -> String.format("%.2f公里", aFloat / 1000f);
    private final static Function<Double, Float> POST_PROCESSOR = d -> (float) (double) d;

    @Override
    protected Fragment getFirstPage() {
        return ReportDetailDistanceDayFragment.getInstance(R.layout.fragment_report_detail_distance_day);
    }

    @Override
    protected Function<Float, String> getAvgFormatter() {
        return AVG_FORMATTER;
    }

    @Override
    protected String getPageType() {
        return RecordType.DISTANCE;
    }

    @Override
    protected int getThemeColor() {
        return R.color.heavyGreen;
    }

    @Override
    protected Function<Double, Float> getDataFormatter() {
        return POST_PROCESSOR;
    }

    @Override
    protected String getTitleText() {
        return "运动距离";
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected Function<Float, String> getYAxisFormatter() {
        return f -> String.format("%.1f km", f / 1000f);
    }


}
