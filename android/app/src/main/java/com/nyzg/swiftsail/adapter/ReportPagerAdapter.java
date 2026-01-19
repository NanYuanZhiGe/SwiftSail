package com.nyzg.swiftsail.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.nyzg.swiftsail.fragment.report.ReportMainFragment;
import com.nyzg.swiftsail.repository.ReportRepository;

import java.time.LocalDate;

public class ReportPagerAdapter extends FragmentStateAdapter {
    public ReportPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        //这里传进来的是position
        //但是我们在缓存中的report是通过day来判断的
        //所以这里需要进行转换
        //0是今天，1是昨天，2是前天，以此类推
        long day = LocalDate.now().toEpochDay() - position;
        return ReportMainFragment.getInstance(day);
    }

    @Override
    public int getItemCount() {
        //这个被初始化为0
        assert ReportRepository.INSTANCE.getPagerCapacity().getValue() != null;
        return ReportRepository.INSTANCE.getPagerCapacity().getValue();
    }
}
