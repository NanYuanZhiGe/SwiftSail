package com.nyzg.swiftsail.fragment.report.detail;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;

import java.time.LocalDate;
import java.util.function.Consumer;

public class ReportDetailCommDayFragment extends ReportDetailDayBaseFragment {

    public static Fragment getInstance(int layoutResource) {
        Fragment fragment = new ReportDetailCommDayFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(LAYOUT_KEY, layoutResource);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected void init(View father) {
    }

    @Override
    protected boolean hasDateSelector() {
        return true;
    }

    @Override
    protected Consumer<LocalDate> getDateSelectorCallback() {
        return super.getDateSelectorCallback();
    }
}
