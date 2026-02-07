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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.fragment.report.pull.ReportAddDeviceFragment;
import com.nyzg.swiftsail.fragment.report.pull.ReportMangeDeviceFragment;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.obj.ParsedReport;
import com.nyzg.swiftsail.repository.ReportRepository;
import com.nyzg.swiftsail.repository.SyncRepository;
import com.nyzg.swiftsail.service.ReportSyncService;
import com.nyzg.swiftsail.view.DateSelector;
import com.nyzg.swiftsail.view.ReportCircle;
import com.nyzg.swiftsail.view.ReportMain0Rect;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;


public class ReportMain0Fragment extends Fragment {

    public static Fragment getInstance() {
        return new ReportMain0Fragment();
    }

    private DateSelector date;
    private MyViewModel viewModel;
    private SwipeRefreshLayout refreshLayout;
    private ReportMain0Rect sleepLayout;
    private ReportMain0Rect stepLayout;
    private ReportMain0Rect distanceLayout;
    private ReportMain0Rect consumptionLayout;
    private ReportMain0Rect heartRateLayout;
    private ReportMain0Rect foodLayout;

    private ReportCircle sleepCircle;
    private ReportCircle stepCircle;
    private ReportCircle heartRateCircle;
    private ReportCircle caloricCircle;

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
        //----------------保存用户是否拉起底部的菜单栏-------------------
        SlidingUpPanelLayout slidingUpPanelLayout = father.findViewById(R.id.slidingUpPanel);
        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    viewModel.openBottom = false;
                } else if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    viewModel.openBottom = true;
                }
            }
        });
        if (viewModel.openBottom) {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        } else {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
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
        refreshLayout = father.findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(() -> {
            if (viewModel.selectedDate > 0L) {
                ReportRepository.getInstance().getReportMainAsync(viewModel.selectedDate, () -> refreshLayout.setRefreshing(false));
            } else {
                refreshLayout.setRefreshing(false);
            }
        });
        return father;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //获取当前日期的数据
        ReportRepository.getInstance().getReportMainAsync(date.getDate(), null);
        //根据Report动态更新界面元素
        ReportRepository.getInstance().mainReport.observe(getViewLifecycleOwner(), this::updateUi);
    }

    /**
     * 更新UI
     *
     * @param report 解析好的report
     */
    private void updateUi(ParsedReport report) {
        sleepLayout.updateUi(report.sleepTime, "睡眠得分 · " + report.sleepScore, report.sleepProgress);
        stepLayout.updateUi(report.stepCount, "", report.stepProgress);
        distanceLayout.updateUi(report.stepDistance, "", report.distanceProgress);
        consumptionLayout.updateUi(report.caloric, "", report.caloricProgress);
        heartRateLayout.updateUi(report.heartRange, "静息心率 · " + report.restHeartBeat, report.heartProgress);
        foodLayout.updateUi(report.nutrition, "", report.nutritionProgress);
        sleepCircle.updateUi(report.sleepTime, report.sleepProgress);
        stepCircle.updateUi(report.stepCount, report.stepProgress);
        heartRateCircle.updateUi(report.restHeartBeat, report.heartProgress);
        caloricCircle.updateUi(report.caloric, report.caloricProgress);
    }

    private void initReportMainView(View father) {
        sleepCircle = father.findViewById(R.id.sleep);
        stepCircle = father.findViewById(R.id.step);
        heartRateCircle = father.findViewById(R.id.heart);
        caloricCircle = father.findViewById(R.id.consumtion);
        sleepLayout = father.findViewById(R.id.sleepLayout);
        stepLayout = father.findViewById(R.id.stepLayout);
        distanceLayout = father.findViewById(R.id.distanceLayout);
        consumptionLayout = father.findViewById(R.id.fireLayout);
        heartRateLayout = father.findViewById(R.id.heartLayout);
        foodLayout = father.findViewById(R.id.foodLayout);
        sleepLayout.setOnClickListener(v -> showDetailFragment(ReportDetailSleepFragment.getInstance()));
        stepLayout.setOnClickListener(v -> showDetailFragment(ReportDetailStepFragment.getInstance()));
        distanceLayout.setOnClickListener(v -> showDetailFragment(ReportDetailDistanceFragment.getInstance()));
        consumptionLayout.setOnClickListener(v -> showDetailFragment(ReportDetailCaloricFragment.getInstance()));
        heartRateLayout.setOnClickListener(v -> showDetailFragment(ReportDetailHeartFragment.getInstance()));
        foodLayout.setOnClickListener(v -> showDetailFragment(ReportDetailFoodFragment.getInstance()));
    }

    /**
     * 当用户选择了某一天后，触发这个逻辑，从数据库汇总查询数据返回
     */
    private void onDateSelected(Long epochDay) {
        viewModel.selectedDate = epochDay;
        ReportRepository.getInstance().getReportMainAsync(epochDay, null);
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
        requireParentFragment().getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.reportFragment, ReportAddDeviceFragment.getInstance())
                .addToBackStack(null)
                .commit();
    }

    private void onManageDeviceClicked(View v) {
        requireParentFragment().getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.reportFragment, ReportMangeDeviceFragment.getInstance())
                .addToBackStack(null)
                .commit();
    }

    private void onFoodClicked(View v) {

    }

    private void showDetailFragment(Fragment fragment) {
        requireParentFragment().getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.reportFragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    public static class MyViewModel extends ViewModel {
        public boolean openBottom = false;
        public long selectedDate = -1L;
    }
}