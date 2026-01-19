package com.nyzg.swiftsail.fragment.report;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
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
import com.nyzg.swiftsail.repository.ReportRepository;

public class ReportMain0Fragment extends Fragment {
    public static Fragment getInstance() {
        return new ReportMain0Fragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_report_main_0, container, false);
        /*
        ReportFragment 分为两部分：上半部分的viewPager和下半部分的上滑栏
        其中viewPager中的View是可变的，内容是ReportMainFragment
         */
        View addDevice = father.findViewById(R.id.addDevice);
        View manageDevice = father.findViewById(R.id.mangeDevice);
        View food = father.findViewById(R.id.food);
        addDevice.setOnClickListener(this::onAddDeviceClicked);
        manageDevice.setOnClickListener(this::onManageDeviceClicked);
        food.setOnClickListener(this::onFoodClicked);
        View view = father.findViewById(R.id.sync);
        view.setOnClickListener(this::onDataSyncClicked);
        ViewPager2 viewPager2 = father.findViewById(R.id.reportViewPager2);
        father.post(() -> initViewPager2(viewPager2));
        return father;
    }

    private void onDataSyncClicked(View v) {

    }

    private void onAddDeviceClicked(View v) {
        MainActivity.addFragmentToStackTop(
                requireActivity().getSupportFragmentManager(),
                ReportAddDeviceFragment.getInstance()
        );
    }

    private void onManageDeviceClicked(View v) {

    }

    private void onFoodClicked(View v) {

    }

    /**
     * 这个函数会初始化ViewPager，并尝试同步一次数据。
     * 如果ReportFragment被不断销毁重建，则会多次调用这个函数，进行多次数据同步
     * 不过不用担心这个问题，我设置了ReportFragment会保留在内存中，而且同步数据的任务是幂等的
     * 同时间只能执行一次
     */
    @SuppressLint("NotifyDataSetChanged")
    private void initViewPager2(ViewPager2 viewPager2) {
        //每次ReportFragment创建都会尝试同步一波数据
        ReportRepository.INSTANCE.syncDataAsync();
        //监视数据集的大小变化
        ReportRepository.INSTANCE.getPagerCapacity().observe(
                getViewLifecycleOwner(), capacity -> {
                    if (viewPager2.getAdapter() == null) {
                        return;
                    }
                    //如果数据集大小变了，就通知adapter更新里面的内容
                    viewPager2.getAdapter().notifyDataSetChanged();
                }
        );
        //初始化ViewPager
        viewPager2.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        viewPager2.setAdapter(new ReportPagerAdapter(requireActivity()));
        viewPager2.setOffscreenPageLimit(3);
        viewPager2.setUserInputEnabled(false);
    }
}
