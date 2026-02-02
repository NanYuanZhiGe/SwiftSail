package com.nyzg.swiftsail.fragment.report;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.tabs.TabLayout;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.adapter.ReportDetailAdapter;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.UnsafeButFixProb;
import com.nyzg.swiftsail.dao.RecordTable;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.fragment.BackPressPopFragment;
import com.nyzg.swiftsail.fragment.report.detail.ReportDetailRestPageFragment;
import com.nyzg.swiftsail.obj.Pair;
import com.nyzg.swiftsail.obj.SumType;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

abstract public class ReportDetailBaseFragment extends BackPressPopFragment {

    @Override
    final public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_report_detail_common, container, false);
        TextView titleTxt = father.findViewById(R.id.titleText);
        titleTxt.setText(getTitleText());
        TabLayout tabLayout = father.findViewById(R.id.summarySelector);
        tabLayout.addTab(tabLayout.newTab().setText("天"));
        tabLayout.addTab(tabLayout.newTab().setText("周"));
        tabLayout.addTab(tabLayout.newTab().setText("月"));
        tabLayout.addTab(tabLayout.newTab().setText("年"));
        tabLayout.addTab(tabLayout.newTab().setText("总"));
        father.findViewById(R.id.backspace).setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                .popBackStack());
        ViewPager2 viewPager2 = father.findViewById(R.id.summaries);
        viewPager2.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        viewPager2.setAdapter(new ReportDetailAdapter(
                getFragmentSuppler(),
                getFragmentSize(),
                requireActivity()
        ));
        viewPager2.setUserInputEnabled(false);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                viewPager2.setCurrentItem(position);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        return father;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LinearLayout layout1 = view.findViewById(R.id.layout1);
        TabLayout tabLayout = view.findViewById(R.id.summarySelector);
        ViewPager2 viewPager2 = view.findViewById(R.id.summaries);
        view.post(() -> {
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int needHeight = UnsafeButFixProb.getInnerHeight();
            if (needHeight < 0) {
                needHeight = screenHeight;
            }
            // 获取 layout1 和 TabLayout 的实际高度
            int layout1Height = layout1.getHeight();
            int tabHeight = tabLayout.getHeight();
            // 计算 ViewPager2 应该有的高度
            int viewPagerHeight = needHeight - layout1Height - tabHeight;
            // 设置 ViewPager2 的 LayoutParams
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) viewPager2.getLayoutParams();
            params.height = Math.max(viewPagerHeight, 0); // 防止负数
            viewPager2.setLayoutParams(params);
        });
    }

    protected long getCurrentUserId() {
        User user = LoginRepository.getInstance().currentUser.getValue();
        if (user == null) {
            return 0L;
        }
        return user.id;
    }

    protected Function<Integer, Fragment> getFragmentSuppler() {
        RecordTable recordTable = SQLiteDB.getDatabase(GlobalApplication.getAppContext()).recordTable();
        return position -> {
            switch (position) {
                case 0:
                    return getFirstPage();
                case 1://week,week的逻辑需要单独处理，因为它是获取每天的数据，没有average
                    return ReportDetailRestPageFragment.getInstance(
                            getThemeColor(),
                            funcParam -> {
                                long startDay = funcParam.fromEpoch;
                                long endDay = funcParam.endEpoch;
                                List<Long> queryList = recordTable.getDayRangeExposeValue(
                                        getCurrentUserId(),
                                        getPageType(),
                                        startDay,
                                        endDay
                                );
                                List<BarEntry> result = new ArrayList<>(queryList.size());
                                for (int i = 0; i < queryList.size(); ++i) {
                                    Long temp = queryList.get(i);
                                    if (temp == null) {
                                        result.add(new BarEntry(i, .0f));
                                        continue;
                                    }
                                    result.add(new BarEntry(i, getDataFormatter().apply(Double.valueOf(temp))));
                                }
                                return new Pair<>(startDay, result);
                            },
                            "ReportDetail" + getPageType() + "Week",
                            SumType.WEEK,
                            getAvgFormatter()
                    );
                case 2://month
                    return ReportDetailRestPageFragment.getInstance(
                            getThemeColor(),
                            funcParam -> {
                                long startWeek = funcParam.fromEpoch;
                                long endWeek = funcParam.endEpoch;
                                Pair<Long, List<Double>> pair = recordTable.getWeekRangeAndOffset(
                                        getCurrentUserId(),
                                        getPageType(),
                                        startWeek,
                                        endWeek
                                );
                                return handleQueryData(pair);
                            },
                            "ReportDetail" + getPageType() + "Month",
                            SumType.MONTH,
                            getAvgFormatter()
                    );
                case 3://year
                    return ReportDetailRestPageFragment.getInstance(
                            getThemeColor(),
                            funcParam -> {
                                long startMonth = funcParam.fromEpoch;
                                long endMonth = funcParam.endEpoch;
                                Pair<Long, List<Double>> pair = recordTable.getMonthRangeAndOffset(
                                        getCurrentUserId(),
                                        getPageType(),
                                        startMonth,
                                        endMonth
                                );
                                return handleQueryData(pair);
                            },
                            "ReportDetail" + getPageType() + "Year",
                            SumType.YEAR,
                            getAvgFormatter()
                    );
                default://total
                    return ReportDetailRestPageFragment.getInstance(
                            getThemeColor(),
                            funcParam -> {
                                Pair<Long, List<Double>> pair = recordTable.getYearRangeAndOffset(
                                        getCurrentUserId(),
                                        getPageType()
                                );
                                return handleQueryData(pair);
                            },
                            "ReportDetail" + getPageType() + "Total",
                            SumType.TOTAL,
                            getAvgFormatter()
                    );
            }
        };
    }

    protected Pair<Long, List<BarEntry>> handleQueryData(Pair<Long, List<Double>> pair) {
        List<BarEntry> result = new ArrayList<>(pair.getB().size());
        for (int i = 0; i < pair.getB().size(); ++i) {
            Double temp = pair.getB().get(i);
            if (temp == null) {
                result.add(new BarEntry(i, .0f));
                continue;
            }
            result.add(new BarEntry(i, getDataFormatter().apply(temp)));
        }
        if (pair.getA() == null) {//没有数据
            return new Pair<>(0L, result);
        }
        return new Pair<>(pair.getA(), result);
    }

    protected int getFragmentSize() {
        return 5;
    }

    protected abstract Fragment getFirstPage();

    protected abstract Function<Float, String> getAvgFormatter();

    protected abstract String getPageType();

    protected abstract int getThemeColor();

    protected abstract Function<Double, Float> getDataFormatter();

    protected abstract String getTitleText();
}
