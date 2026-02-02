package com.nyzg.swiftsail.fragment.login;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

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

    private View nav;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_main, container, false);
        initNavManager(father);
        nav = father.findViewById(R.id.nav);
        shrinkNavOnKeyboardStatus();
        return father;
    }

    private void shrinkNavOnKeyboardStatus() {
        if (getActivity() == null) {
            return;
        }
        final View rootView = getActivity().getWindow().getDecorView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getHeight();//屏幕的高度
            int keypadHeight = screenHeight - r.bottom;
            UnsafeButFixProb.innerFragmentHeight.setValue(r.bottom);
            boolean isKeyboardShown = keypadHeight > screenHeight * 0.15;
            if (isKeyboardShown) {
                nav.setVisibility(View.GONE);
            } else {
                nav.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View viewpager = view.findViewById(R.id.mainViewPager);
        viewpager.post(() -> UnsafeButFixProb.innerFragmentHeight.setValue(viewpager.getHeight()));
    }

    private void initNavManager(View father) {
        navManager.addView(father.findViewById(R.id.mainViewPager), father.findViewById(R.id.nav), this.requireActivity());
    }
}
