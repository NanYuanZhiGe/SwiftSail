package com.nyzg.swiftsail.fragment.main;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.adapter.ReportPagerAdapter;
import com.nyzg.swiftsail.fragment.mine.MineMainFragment;
import com.nyzg.swiftsail.fragment.report.ReportAddDeviceFragment;
import com.nyzg.swiftsail.fragment.report.ReportMain0Fragment;
import com.nyzg.swiftsail.repository.ReportRepository;

public class ReportFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.layout_report, container, false);
        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.reportFragment, ReportMain0Fragment.getInstance())
                .commit();
        return father;
    }
}