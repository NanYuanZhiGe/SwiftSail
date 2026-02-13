package com.nyzg.swiftsail.fragment.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.fragment.login.LoginFragment;
import com.nyzg.swiftsail.fragment.login.MainFragment;
import com.nyzg.swiftsail.obj.SucceedOrNot;
import com.nyzg.swiftsail.repository.LoginRepository;
import com.nyzg.swiftsail.repository.RecordRepository;

public class WaitingFragment extends Fragment {

    public static Fragment getInstance() {
        return new WaitingFragment();
    }

    TextView progressText;
    TextView detailText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_main_waiting, container, false);
        progressText = father.findViewById(R.id.progressText);
        detailText = father.findViewById(R.id.detailMsg);
        progressText.setText("正在登录");
        detailText.setText("使用上一次的登录账户");
        return father;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkAndDoLogin();
    }

    private void checkAndDoLogin() {
        assert getActivity() != null;
        LoginRepository.getInstance()
                .tryLastLoginAsync(msg -> detailText.post(()->detailText.setText(msg)))
                .thenAcceptAsync(result -> {
                    if (result.getA() == SucceedOrNot.FAIL) {
                        //使用上一次的登录账户登录失败，添加登录页面，
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainFragment, MainFragment.getInstance())
                                .commit();
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainFragment, LoginFragment.newInstance())
                                .addToBackStack(null)
                                .commit();
                    } else {
                        //使用上一次的登录账户登录成功，直接进入主界面
                        getActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.mainFragment, MainFragment.getInstance())
                                .commit();
                    }
                    progressText.setText(result.getB());
                    //此时LoginRepository已经成功更新了currentUser
                    RecordRepository.getInstance().updateUserOnChange();
                }, ContextCompat.getMainExecutor(GlobalApplication.getAppContext()));
    }
}
