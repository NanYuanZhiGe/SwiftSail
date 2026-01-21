package com.nyzg.swiftsail.fragment.report;

import android.annotation.SuppressLint;

import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.dao.RecordTableBase;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailCommDayFragment;

import java.util.function.Function;

public class ReportDetailCaloricFragment extends ReportDetailBaseFragment {
    public static Fragment getInstance() {
        return new ReportDetailCaloricFragment();
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
        return "Caloric";
    }

    @Override
    protected int getThemeColor() {
        return R.color.heavyGreen;
    }

    @Override
    protected RecordTableBase getRecordTable() {
        return SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordConsumptionTable();
    }

    @Override
    protected Function<Double, Float> getDataFormatter() {
        return POST_PROCESSOR;
    }

    @Override
    protected String getTitleText() {
        return "消耗热量";
    }

}
