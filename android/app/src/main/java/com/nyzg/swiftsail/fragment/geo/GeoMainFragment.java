package com.nyzg.swiftsail.fragment.geo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.UnsafeButFixProb;
import com.nyzg.swiftsail.repository.LoginRepository;

public class GeoMainFragment extends Fragment {
    public static Fragment getInstance() {
        return new GeoMainFragment();
    }

    private TabLayout tabLayout;
    private ViewPager2 pager2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_geo_main, container, false);
        ImageView headImage=father.findViewById(R.id.headImage);
        ImageView userHead=father.findViewById(R.id.userHead);
        LoginRepository.getInstance().watchHeadIcon(getViewLifecycleOwner(),headImage,requireContext());
        LoginRepository.getInstance().watchHeadIcon(getViewLifecycleOwner(),userHead,requireContext());
        //解决innerFragment drawerFragment的MeasureSpec.EXACTLY问题
        UnsafeButFixProb.innerFragmentHeight.observe(getViewLifecycleOwner(), height -> {
            DrawerLayout drawerLayout = father.findViewById(R.id.drawerLayout);
            ViewGroup.LayoutParams layoutParams = drawerLayout.getLayoutParams();
            layoutParams.height = height;
            drawerLayout.setLayoutParams(layoutParams);
        });
        //打开新的fragment
        //聊天界面
        father.findViewById(R.id.notification).setOnClickListener(v -> requireParentFragment()
                .getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.geoFragment, GeoNotificationFragment.getInstance())
                .addToBackStack(null)
                .commit());
        //匹配界面
        father.findViewById(R.id.add).setOnClickListener(v -> requireParentFragment()
                .getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.geoFragment, GeoAddMatchFragment.getInstance())
                .addToBackStack(null)
                .commit());
        //点击头像打开drawer
        View imageContainer = father.findViewById(R.id.headIconContainer);
        imageContainer.setOnClickListener(v -> {
            DrawerLayout drawerLayout = father.findViewById(R.id.drawerLayout);
            drawerLayout.openDrawer(GravityCompat.END, true);
        });
        tabLayout = father.findViewById(R.id.tab);
        pager2 = father.findViewById(R.id.pager2);
        tabLayout.addTab(tabLayout.newTab().setText("已完成"));
        tabLayout.addTab(tabLayout.newTab().setText("进行中"));
        tabLayout.addTab(tabLayout.newTab().setText("在匹配"));
        pager2.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        pager2.setAdapter(new ViewPager2Adapter(getChildFragmentManager(), getLifecycle()));
        pager2.setUserInputEnabled(false);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                pager2.setCurrentItem(position);
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
        pager2.post(() -> {
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
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) pager2.getLayoutParams();
            params.height = Math.max(viewPagerHeight, 0); // 防止负数
            pager2.setLayoutParams(params);
        });
    }

    private static class ViewPager2Adapter extends FragmentStateAdapter {
        public ViewPager2Adapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
            super(fragmentManager, lifecycle);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 0 -> GeoMatchDoneFragment.getInstance();
                case 1 -> GeoMatchHalfFragment.getInstance();
                default -> GeoMatchOnFragment.getInstance();
            };
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
