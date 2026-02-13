package com.nyzg.swiftsail.fragment.report;

import android.annotation.SuppressLint;

import androidx.fragment.app.Fragment;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailCommDayFragment;
import java.util.function.Function;

public class ReportDetailFoodFragment extends ReportDetailBaseFragment {
    public static Fragment getInstance() {
        return new ReportDetailFoodFragment();
    }

    @SuppressLint("DefaultLocale")
    private final static Function<Float, String> AVG_FORMATTER = aFloat -> String.format("%.2f卡", aFloat);
    private final static Function<Double, Float> POST_PROCESSOR = d -> (float) (double) d;

    @Override
    protected Fragment getFirstPage() {
        return ReportDetailCommDayFragment.getInstance(R.layout.fragment_report_detail_day_common);
    }

    @Override
    protected Function<Float, String> getAvgFormatter() {
        return AVG_FORMATTER;
    }

    @Override
    protected String getPageType() {
        return RecordType.FOOD;
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
        return "饮食";
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected Function<Float, String> getYAxisFormatter() {
        return f->String.format("%.0f",f);
    }

}
