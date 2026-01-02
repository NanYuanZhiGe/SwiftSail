package com.nyzg.swiftsail.bean;


import static com.nyzg.swiftsail.R.id.nac_comprehen;
import static com.nyzg.swiftsail.R.id.nav_geo;
import static com.nyzg.swiftsail.R.id.nav_mine;
import static com.nyzg.swiftsail.R.id.nav_record;
import static com.nyzg.swiftsail.R.id.nav_report;

import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.nyzg.swiftsail.adapter.MainPagerAdapter;

public class NavManager {
    static private volatile NavManager self;

    public static NavManager getInstance() {
        if (self != null) {
            return self;
        }
        synchronized (NavManager.class) {
            if (self != null) {
                return self;
            }
            self = new NavManager();
        }
        return self;
    }

    private NavManager() {
    }

    BottomNavigationView bottomNavigationView;

    public void addView(ViewPager2 viewPager2, BottomNavigationView view, FragmentActivity activity) {
        viewPager2.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        viewPager2.setAdapter(new MainPagerAdapter(activity));
        viewPager2.setOffscreenPageLimit(4);
        viewPager2.setUserInputEnabled(false);
        this.bottomNavigationView = view;
        view.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int currentItem = viewPager2.getCurrentItem();
            if (id == nav_record && currentItem != 0) {
                viewPager2.setCurrentItem(0);
            } else if (id == nav_report && currentItem != 1) {
                viewPager2.setCurrentItem(1);
            } else if (id == nac_comprehen && currentItem != 2) {
                viewPager2.setCurrentItem(2);
            } else if (id == nav_geo && currentItem != 3) {
                viewPager2.setCurrentItem(3);
            } else if (id == nav_mine && currentItem != 4) {
                viewPager2.setCurrentItem(4);
            }
            return true;
        });
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                changeNav(position);
            }
        });
    }

    public void changeNav(int position) {
        int id = nav_record;
        switch (position) {
            case 1:
                id = nav_report;
                break;
            case 2:
                id = nac_comprehen;
                break;
            case 3:
                id = nav_geo;
                break;
            case 4:
                id = nav_mine;
        }
        bottomNavigationView.setSelectedItemId(id);
    }
}