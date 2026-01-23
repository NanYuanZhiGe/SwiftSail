package com.nyzg.swiftsail.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nyzg.swiftsail.bean.UnsafeButFixProb;

public class InnerFragment extends BackPressPopFragment {
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        params.height = Math.min(Math.max(UnsafeButFixProb.innerFragmentHeight, 0), screenHeight);
        view.setLayoutParams(params);
    }
}
