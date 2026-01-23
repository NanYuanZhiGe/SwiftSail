package com.nyzg.swiftsail.fragment.report;

import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.RecordType;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailCommDayFragment;
import java.util.function.Function;

public class ReportDetailStepFragment extends ReportDetailBaseFragment {
    public static Fragment getInstance() {
        return new ReportDetailStepFragment();
    }

    private final static Function<Float, String> AVG_FORMATTER = aFloat -> ((int) (float) aFloat) + "";
    private final static Function<Double, Float> POST_PROCESSOR = d -> (float) (Math.round(d));

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
        return RecordType.STEP;
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
        return "步数";
    }
}
