package com.nyzg.swiftsail.adapter;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.nyzg.swiftsail.fragment.main.ComprehenFragment;
import com.nyzg.swiftsail.fragment.main.GeoFragment;
import com.nyzg.swiftsail.fragment.main.MineFragment;
import com.nyzg.swiftsail.fragment.main.RecordFragment;
import com.nyzg.swiftsail.fragment.main.ReportFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = new RecordFragment();
        fragment = switch (position) {
            case 1 -> new ReportFragment();
            case 2 -> new ComprehenFragment();
            case 3 -> new GeoFragment();
            case 4 -> new MineFragment();
            default -> fragment;
        };
        return fragment;
    }


    @Override
    public int getItemCount() {
        return 5;
    }
}
