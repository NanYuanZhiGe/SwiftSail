package com.nyzg.swiftsail.fragment.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.NavManager;
import com.nyzg.swiftsail.bean.UnsafeButFixProb;

public class MainFragment extends Fragment {
    private final NavManager navManager = NavManager.getInstance();

    public static Fragment getInstance() {
        return new MainFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_main, container, false);
        initNavManager(father);
        return father;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View viewpager = view.findViewById(R.id.mainViewPager);
        viewpager.post(() -> UnsafeButFixProb.innerFragmentHeight = viewpager.getHeight());
    }

    private void initNavManager(View father) {
        navManager.addView(father.findViewById(R.id.mainViewPager), father.findViewById(R.id.nav), this.requireActivity());
    }
}
