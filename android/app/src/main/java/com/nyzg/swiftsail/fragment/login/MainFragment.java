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

public class MainFragment extends Fragment {
    private final NavManager navManager = NavManager.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_main, container, false);
        initNavManager(father);
        return father;
    }

    private void initNavManager(View father) {
        navManager.addView(father.findViewById(R.id.mainViewPager), father.findViewById(R.id.nav), this.requireActivity());
    }
}
