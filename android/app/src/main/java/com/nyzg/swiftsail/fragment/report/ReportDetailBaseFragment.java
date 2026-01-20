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

import com.google.android.material.tabs.TabLayout;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.adapter.ReportDetailAdapter;
import com.nyzg.swiftsail.bean.UnsafeButFixProb;
import com.nyzg.swiftsail.fragment.BackPressQuitFragment;

import java.util.function.Function;

abstract public class ReportDetailBaseFragment extends BackPressQuitFragment {

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
            int needHeight = UnsafeButFixProb.innerFragmentHeight;
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

    protected abstract String getTitleText();

    protected abstract Function<Integer, Fragment> getFragmentSuppler();

    protected abstract int getFragmentSize();
}
