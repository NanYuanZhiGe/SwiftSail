package com.nyzg.swiftsail.fragment.report;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.fragment.report.pull.ReportAddDeviceFragment;
import com.nyzg.swiftsail.fragment.report.pull.ReportMangeDeviceFragment;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.repository.ReportRepository;
import com.nyzg.swiftsail.repository.SyncRepository;
import com.nyzg.swiftsail.service.ReportSyncService;
import com.nyzg.swiftsail.view.DateSelector;


public class ReportMain0Fragment extends Fragment {

    public static Fragment getInstance() {
        return new ReportMain0Fragment();
    }

    private DateSelector date;
    private MyViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_report_main_0, container, false);
        View addDevice = father.findViewById(R.id.addDevice);
        View manageDevice = father.findViewById(R.id.mangeDevice);
        View food = father.findViewById(R.id.food);
        addDevice.setOnClickListener(this::onAddDeviceClicked);
        manageDevice.setOnClickListener(this::onManageDeviceClicked);
        food.setOnClickListener(this::onFoodClicked);
        View view = father.findViewById(R.id.sync);
        view.setOnClickListener(this::onDataSyncClicked);
        View reportMainView = father.findViewById(R.id.reportMainView);
        initReportMainView(reportMainView);
        //点击选择日期来展示数据
        date = father.findViewById(R.id.date);
        date.setOnDateSelectedFunc(this::onDateSelected);
        if (viewModel.selectedDate > 0L) {//说明这个fragment重建过
            date.setDate(viewModel.selectedDate);
        }
        return father;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //获取当前日期的数据
        ReportRepository.getInstance().getReportMainAsync(date.getDate());
        //根据Report动态更新界面元素
        ReportRepository.getInstance()
                .mainReport.observe(getViewLifecycleOwner(), report -> {
                    setTextNullAtNoData(view.findViewById(R.id.sleepTime), report.sleepTime);
                    setTextNullAtNoData(view.findViewById(R.id.sleepScore), report.sleepScore);
                    setTextNullAtNoData(view.findViewById(R.id.stepCount), report.stepCount);
                    setTextNullAtNoData(view.findViewById(R.id.distanceCount), report.stepDistance);
                    setTextNullAtNoData(view.findViewById(R.id.fireCount), report.caloric);
                    setTextNullAtNoData(view.findViewById(R.id.heartRange), report.heartRange);
                    setTextNullAtNoData(view.findViewById(R.id.hearCalm), report.restHeartBeat);
                    setTextNullAtNoData(view.findViewById(R.id.foodCount), report.nutrition);
                });
    }

    private void setTextNullAtNoData(View v, String s) {
        if (s == null) {
            ((TextView) v).setText("无数据");
            return;
        }
        ((TextView) v).setText(s);
    }

    /**
     * 当用户选择了某一天后，触发这个逻辑，从数据库汇总查询数据返回
     */
    private void onDateSelected(Long epochDay) {
        viewModel.selectedDate = epochDay;
        ReportRepository.getInstance().getReportMainAsync(epochDay);
    }

    private void onDataSyncClicked(View v) {
        //正在运行
        if (!SyncRepository.getInstance().onSync.compareAndSet(false, true)) {
            SyncRepository.getInstance().notificationPair.postValue(new Pair<>("您已经在同步数据了", null));
            return;
        }
        Intent intent = new Intent(requireContext(), ReportSyncService.class);
        requireContext().startForegroundService(intent);
    }


    private void onAddDeviceClicked(View v) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.reportFragment, ReportAddDeviceFragment.getInstance())
                .addToBackStack(null)
                .commit();
    }

    private void onManageDeviceClicked(View v) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.reportFragment, ReportMangeDeviceFragment.getInstance())
                .addToBackStack(null)
                .commit();
    }

    private void onFoodClicked(View v) {

    }

    private void initReportMainView(View father) {
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
    }

    private void showDetailFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.reportFragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    public static class MyViewModel extends ViewModel {
        public long selectedDate = -1L;
    }
}