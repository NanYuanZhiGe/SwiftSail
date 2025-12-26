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
        switch (position) {
            case 1:
                fragment = new ReportFragment();
                break;
            case 2:
                fragment = new ComprehenFragment();
                break;
            case 3:
                fragment = new GeoFragment();
                break;
            case 4:
                fragment = new MineFragment();
        }
        return fragment;
    }


    @Override
    public int getItemCount() {
        return 5;
    }
}
