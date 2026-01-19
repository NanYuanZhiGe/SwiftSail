package com.nyzg.swiftsail.adapter;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.net.Inet4Address;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ReportDetailAdapter extends FragmentStateAdapter {
    private final int size;
    private final Function<Integer,Fragment> fragmentSupplier;

    public ReportDetailAdapter(
            Function<Integer,Fragment> fragmentSupplier,
            int size,
            @NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.size = size;
        this.fragmentSupplier = fragmentSupplier;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentSupplier.apply(position);
    }

    @Override
    public int getItemCount() {
        return size;
    }
}
