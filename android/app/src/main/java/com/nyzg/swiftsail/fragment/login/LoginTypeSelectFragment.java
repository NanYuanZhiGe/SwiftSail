package com.nyzg.swiftsail.fragment.login;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.repository.LoginRepository;


@SuppressLint("ClickableViewAccessibility")
public class LoginTypeSelectFragment extends Fragment {
    User user;

    public static Fragment newInstance() {
        return new LoginTypeSelectFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = LoginRepository.getInstance().onLoginUser.getValue();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (user == null) {
            Toast.makeText(GlobalApplication.getAppContext(), "当前设备没有账户信息", Toast.LENGTH_LONG).show();
        }
        View father = inflater.inflate(R.layout.framgent_login_select, container, false);
        onClickSwitchFragment(father.findViewById(R.id.mailLogin), MailLoginFragment.newInstance());
        onClickSwitchFragment(father.findViewById(R.id.passwordLogin), PasswordLoginFragment.newInstance());
        onClickSwitchFragment(father.findViewById(R.id.keyLogin), KeyLoginFragment.newInstance());
        return father;
    }

    private void onClickSwitchFragment(View view, Fragment fragment) {
        view.setOnTouchListener((v, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    view.setPressed(true);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    view.setPressed(false);
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.mainFragment, fragment)
                            .addToBackStack(null)
                            .commit();
                    break;
            }
            return true;
        });
    }
}
