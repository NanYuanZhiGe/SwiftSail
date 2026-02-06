package com.nyzg.swiftsail.fragment.login;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.boybeak.skbglobal.SoftKeyboardGlobal;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.NavManager;
import com.nyzg.swiftsail.bean.UnsafeButFixProb;

public class MainFragment extends Fragment {
    private final NavManager navManager = NavManager.getInstance();

    public static Fragment getInstance() {
        return new MainFragment();
    }

    View nav;
    View root;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_main, container, false);
        initNavManager(father);
        nav = father.findViewById(R.id.mainSelector);
        root = father;
        return father;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View viewpager = view.findViewById(R.id.mainViewPager);
        viewpager.post(() -> UnsafeButFixProb.innerFragmentHeight.setValue(viewpager.getHeight()));
        SoftKeyboardGlobal.INSTANCE.addSoftKeyboardCallback(new SoftKeyboardGlobal.SoftKeyboardCallback() {
            @Override
            public void onOpen(int i) {
                nav.setVisibility(GONE);
                root.post(() -> {
                    int availableHeight = root.getHeight() - i;
                    UnsafeButFixProb.innerFragmentHeight.setValue(availableHeight);
                });
            }

            @Override
            public void onClose() {
                nav.setVisibility(VISIBLE);
                nav.post(() -> {
                    int availableHeight = root.getHeight() - nav.getHeight();
                    UnsafeButFixProb.innerFragmentHeight.setValue(availableHeight);
                });
            }

            @Override
            public void onHeightChanged(int i) {

            }
        });
    }


    private void initNavManager(View father) {
        navManager.addView(father.findViewById(R.id.mainViewPager), father.findViewById(R.id.nav), this.requireActivity());
    }
}
