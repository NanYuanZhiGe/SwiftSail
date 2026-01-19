package com.nyzg.swiftsail.fragment.report;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.viewmodel.ReportViewModel;

public class ReportMainFragment extends Fragment {

    private static final String DAY_KEY = "day_key";
    private Long day = 0L;
    private ReportViewModel reportViewModel;

    public static Fragment getInstance(long key) {
        Fragment fragment = new ReportMainFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(DAY_KEY, key);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //不可能为空的，因为构造函数私有，只能通过getInstance来初始化
        assert getArguments() != null;
        day = getArguments().getLong(DAY_KEY);
        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_report_main, container, false);
        reportViewModel.getMutableReport().observe(getViewLifecycleOwner(), report -> {

        });
        View sleepLayout = father.findViewById(R.id.sleepLayout);
        View stepLayout = father.findViewById(R.id.stepLayout);
        View distanceLayout = father.findViewById(R.id.distanceLayout);
        View caloricLayout = father.findViewById(R.id.fireLayout);
        View heartLayout = father.findViewById(R.id.heartLayout);
        View foodLayout = father.findViewById(R.id.foodLayout);
        sleepLayout.setOnClickListener(v -> showDetailFragment(ReportDetailSleepFragment.getInstance()));
        stepLayout.setOnClickListener(v -> showDetailFragment(ReportDetailStepFragment.getInstance()));
        distanceLayout.setOnClickListener(v -> showDetailFragment(ReportDetailDistanceFragment.getInstance()));
        caloricLayout.setOnClickListener(v -> showDetailFragment(ReportDetailCaloricFragment.getInstance()));
        heartLayout.setOnClickListener(v -> showDetailFragment(ReportDetailHeartFragment.getInstance()));
        foodLayout.setOnClickListener(v -> showDetailFragment(ReportDetailFoodFragment.getInstance()));
        return father;
    }

    private void showDetailFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.reportFragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //延迟3s再拉取数据，避免用户频繁切换进行数据同步
        view.postDelayed(() -> {
            if (isAdded() && !isDetached() && !isRemoving()) {
                reportViewModel.loadDataAsync(day);
            }
        }, 3000);//单位是毫秒
    }
}