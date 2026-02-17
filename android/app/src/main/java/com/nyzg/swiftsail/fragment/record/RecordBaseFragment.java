package com.nyzg.swiftsail.fragment.record;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;

public abstract class RecordBaseFragment extends Fragment {

    @Override
    final public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (containsBackward()) {
            view.findViewById(R.id.backward).setOnClickListener(v -> {
                if (needToAlert()) {//如果需要进行提示
                    new AlertDialog.Builder(requireContext())
                            .setTitle("")
                            .setMessage("您本次的运动记录未保存，需要保存它吗？")
                            .setPositiveButton("保存", (dialog, which) -> {
                                onSaveQuit();
                                requireActivity().getSupportFragmentManager().popBackStack();
                            })
                            // 取消则什么也不做
                            .setNegativeButton("不保存", (d, w) -> requireActivity().getSupportFragmentManager().popBackStack())
                            .show();
                } else {//否则直接返回
                    requireActivity().getSupportFragmentManager()
                            .popBackStack();
                }
            });
        }
        // 取消则什么也不做
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!needToAlert()) {
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    return;
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("")
                        .setMessage("您本次的运动记录未保存，需要保存它吗？")
                        .setPositiveButton("保存", (dialog, which) -> {
                            onSaveQuit();
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        })
                        // 取消则什么也不做
                        .setNegativeButton("不保存", (d, w) -> {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        })
                        .show();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(callback);
        myOnViewCreated();
    }

    protected void myOnViewCreated() {
    }


    protected abstract boolean needToAlert();

    /**
     * 要求findViewById(R.id.backward)存在
     */
    protected abstract boolean containsBackward();

    /**
     * 注意异步方法要completable
     */
    protected abstract void onSaveQuit();

}
