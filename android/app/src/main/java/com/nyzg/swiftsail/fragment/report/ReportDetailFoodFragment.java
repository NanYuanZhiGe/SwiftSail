package com.nyzg.swiftsail.fragment.report;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailCommDayFragment;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailPageDayFragment;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.obj.SumType;

import java.util.ArrayList;
import java.util.function.Function;

public class ReportDetailFoodFragment extends ReportDetailBaseFragment {
    public static Fragment getInstance() {
        return new ReportDetailFoodFragment();
    }

    @SuppressLint("DefaultLocale")
    private final static Function<Float, String> AVG_FORMATTER = aFloat -> String.format("%.2f卡", aFloat);

    @Override
    protected String getTitleText() {
        return "饮食";
    }

    @Override
    protected Function<Integer, Fragment> getFragmentSuppler() {
        return position -> {
            switch (position) {
                case 0:
                    return ReportDetailCommDayFragment.getInstance(R.layout.fragment_report_detail_day_common);
                case 1://week
                    return ReportDetailPageDayFragment.getInstance(
                            R.color.heavyPink,
                            funcParam -> {
                                return new Pair<>(0L, new ArrayList<>(0));
                            },
                            "ReportDetailFoodWeek",
                            SumType.WEEK,
                            AVG_FORMATTER
                    );
                case 2://month
                    return ReportDetailPageDayFragment.getInstance(
                            R.color.heavyPink,
                            funcParam -> {
                                return new Pair<>(0L, new ArrayList<>(0));
                            },
                            "ReportDetailFoodMonth",
                            SumType.MONTH,
                            AVG_FORMATTER
                    );
                case 3://year
                    return ReportDetailPageDayFragment.getInstance(
                            R.color.heavyPink,
                            funcParam -> {
                                return new Pair<>(0L, new ArrayList<>(0));
                            },
                            "ReportDetailFoodYear",
                            SumType.YEAR,
                            AVG_FORMATTER
                    );
                default://total
                    return ReportDetailPageDayFragment.getInstance(
                            R.color.heavyPink,
                            funcParam -> {
                                return new Pair<>(0L, new ArrayList<>(0));
                            },
                            "ReportDetailFoodTotal",
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
