package com.nyzg.swiftsail.fragment.report;

import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailCommDayFragment;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailPageDayFragment;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.obj.SumType;

import java.util.ArrayList;
import java.util.function.Function;

public class ReportDetailHeartFragment extends ReportDetailBaseFragment {
    static public Fragment getInstance() {
        return new ReportDetailHeartFragment();
    }

    private final static Function<Float, String> AVG_FORMATTER = aFloat -> ((int) (float) aFloat) + " bpm";

    @Override
    protected String getTitleText() {
        return "心率";
    }

    @Override
    protected Function<Integer, Fragment> getFragmentSuppler() {
        return position -> {
            switch (position) {
                case 0:
                    return ReportDetailCommDayFragment.getInstance(R.layout.fragment_report_detail_day_common);
                case 1://week
                    return ReportDetailPageDayFragment.getInstance(
                            R.color.heavyBlue,
                            funcParam -> {
                                return new Pair<>(0L, new ArrayList<>(0));
                            },
                            "ReportDetailHeartWeek",
                            SumType.WEEK,
                            AVG_FORMATTER
                    );
                case 2://month
                    return ReportDetailPageDayFragment.getInstance(
                            R.color.heavyBlue,
                            funcParam -> {
                                return new Pair<>(0L, new ArrayList<>(0));
                            },
                            "ReportDetailHeartMonth",
                            SumType.MONTH,
                            AVG_FORMATTER
                    );
                case 3://year
                    return ReportDetailPageDayFragment.getInstance(
                            R.color.heavyBlue,
                            funcParam -> {
                                return new Pair<>(0L, new ArrayList<>(0));
                            },
                            "ReportDetailHeartYear",
                            SumType.YEAR,
                            AVG_FORMATTER
                    );
                default://total
                    return ReportDetailPageDayFragment.getInstance(
                            R.color.heavyBlue,
                            funcParam -> {
                                return new Pair<>(0L, new ArrayList<>(0));
                            },
                            "ReportDetailHeartTotal",
                            SumType.TOTAL,
                            AVG_FORMATTER
                    );
            }
        };
    }

    @Override
    protected int getFragmentSize() {
        return 5;
    }
}
