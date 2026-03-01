package com.nyzg.swiftsail.fragment.geo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.NetWorkBuilder;
import com.nyzg.swiftsail.bean.NetWorkHandler;
import com.nyzg.swiftsail.bean.SQLiteDB;
import com.nyzg.swiftsail.bean.ServerURL;
import com.nyzg.swiftsail.bean.UnsafeButFixProb;
import com.nyzg.swiftsail.dbobj.PersonalData;
import com.nyzg.swiftsail.repository.GeoRepository;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.repository.NetDataRepository;
import com.nyzg.swiftsail.view.ScoreRankLayout;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class GeoMainFragment extends Fragment {
    public static Fragment getInstance() {
        return new GeoMainFragment();
    }

    private TabLayout tabLayout;
    private ViewPager2 pager2;

    @SuppressLint("DefaultLocale")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_geo_main, container, false);
        ImageView headImage = father.findViewById(R.id.headImage);
        ImageView userHead = father.findViewById(R.id.userHead);
        TextView userName = father.findViewById(R.id.userName);
        LoginRepository.getInstance().currentUser.observe(getViewLifecycleOwner(), usr -> {
            if (usr == null) {
                return;
            }
            userName.setText(usr.nickName);
        });
        NetDataRepository.getInstance().headIconNotifier.observe(getViewLifecycleOwner(), url -> {
            Glide.with(requireContext())
                    .load(url)
                    .error(R.drawable.image)
                    .into(headImage);
            Glide.with(requireContext())
                    .load(url)
                    .error(R.drawable.image)
                    .into(userHead);
        });
        //解决innerFragment drawerFragment的MeasureSpec.EXACTLY问题
        UnsafeButFixProb.innerFragmentHeight.observe(getViewLifecycleOwner(), height -> {
            DrawerLayout drawerLayout = father.findViewById(R.id.drawerLayout);
            ViewGroup.LayoutParams layoutParams = drawerLayout.getLayoutParams();
            layoutParams.height = height;
            drawerLayout.setLayoutParams(layoutParams);
        });
        //打开新的fragment
        father.findViewById(R.id.editUserData).setOnClickListener(v -> requireParentFragment()
                .getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.geoFragment, GeoEditUserData.getInstance())
                .addToBackStack(null)
                .commit()
        );
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
        //获取个人信息，保存至repository中
        LoginRepository.getInstance().currentUser.observe(getViewLifecycleOwner(),
                user -> CompletableFuture.runAsync(() -> {
                    try {
                        NetWorkHandler.handleNetRespAfterLogin(
                                null,
                                NetWorkBuilder.doChunkRequest(NetWorkBuilder.buildJsonRequestJwt(
                                        new URL(ServerURL.STRING_GET_PERSONAL_INFO + user.id), ServerURL.GET, null)
                                ),
                                msg -> {
                                    //找不到就拿数据库里面的
                                    PersonalData personalData = SQLiteDB.getDatabase(GlobalApplication.getAppContext())
                                            .personalDataTable()
                                            .selectPersonData(user.id).stream().findFirst().orElse(null);
                                    if (personalData == null) {//数据库里面没有就用默认的
                                        return;
                                    }
                                    GeoRepository.getInstance().personalData.postValue(personalData);
                                },
                                resp -> {
                                    //如果有，先更新数据库
                                    SQLiteDB.getDatabase(GlobalApplication.getAppContext())
                                            .personalDataTable()
                                            .insertDataSafe(
                                                    user.id,
                                                    resp.appellation,
                                                    resp.gender,
                                                    resp.description,
                                                    resp.promiseScore,
                                                    resp.inTimeScore,
                                                    resp.levelScore,
                                                    resp.cooperationScore,
                                                    resp.communicateScore,
                                                    resp.bkImage,
                                                    resp.layoutImage
                                            );
                                    //然后更新UI
                                    GeoRepository.getInstance().personalData.postValue(resp);
                                },
                                PersonalData.class
                        );
                    } catch (Exception ignore) {//不会发生
                    }
                }));
        TextView userSimpleData = father.findViewById(R.id.userSimpleData);
        TextView descContent = father.findViewById(R.id.descContent);
        ScoreRankLayout keepPromise = father.findViewById(R.id.keepPromise);
        ScoreRankLayout alwaysInTime = father.findViewById(R.id.alwaysInTime);
        ScoreRankLayout levelMatch = father.findViewById(R.id.levelMatch);
        ScoreRankLayout cooperation = father.findViewById(R.id.cooperation);
        ScoreRankLayout friendly = father.findViewById(R.id.friendly);
        TextView average = father.findViewById(R.id.average);
        ImageView background = father.findViewById(R.id.background);
        ImageView[] imageViews = new ImageView[]{
                father.findViewById(R.id.image1), father.findViewById(R.id.image2), father.findViewById(R.id.image3),
                father.findViewById(R.id.image4), father.findViewById(R.id.image5), father.findViewById(R.id.image6),
                father.findViewById(R.id.image7), father.findViewById(R.id.image8), father.findViewById(R.id.image9)
        };
        GeoRepository.getInstance().personalData.observe(getViewLifecycleOwner(), personalData -> {
            if (personalData == null) {
                return;
            }
            userSimpleData.setText(String.format("%s | %s", personalData.appellation, personalData.gender==0?"男":"女"));
            descContent.setText(personalData.description);
            keepPromise.setScore(personalData.promiseScore / 2f);
            alwaysInTime.setScore(personalData.inTimeScore / 2f);
            levelMatch.setScore(personalData.levelScore / 2f);
            cooperation.setScore(personalData.cooperationScore / 2f);
            friendly.setScore(personalData.communicateScore / 2f);
            average.setText(String.format("%.1f/5",
                    (personalData.promiseScore + personalData.inTimeScore
                            + personalData.levelScore + personalData.cooperationScore + personalData.communicateScore) / 10f)
            );
            if (personalData.bkImage != null) {
                Glide.with(GlobalApplication.getAppContext())
                        .load(ServerURL.BASE_IMAGE_URL + personalData.bkImage)
                        .error(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.fragment_geo_user_bk_default))
                        .into(background);
            }
            if (personalData.layoutImage != null) {
                String[] urls = personalData.layoutImage.split("\\|");
                for (int i = 0; i < urls.length; ++i) {
                    Glide.with(GlobalApplication.getAppContext())
                            .load(ServerURL.BASE_IMAGE_URL + urls[i])
                            .error(ContextCompat.getDrawable(GlobalApplication.getAppContext(), R.drawable.image))
                            .into(imageViews[i]);
                }
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
