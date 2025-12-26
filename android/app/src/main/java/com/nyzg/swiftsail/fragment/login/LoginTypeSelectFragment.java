package com.nyzg.swiftsail.fragment.login;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.GlobalApplication;
import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.dbobj.User;


@SuppressLint("ClickableViewAccessibility")
public class LoginTypeSelectFragment extends Fragment {
    User user;
    public static final String USER_KEY = "user_key";

    public static Fragment newInstance(User user) {
        Fragment fragment = new LoginTypeSelectFragment();
        if (user != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(USER_KEY, user);
            fragment.setArguments(bundle);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            user = getArguments().getParcelable(USER_KEY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (user == null) {
            Toast.makeText(GlobalApplication.getAppContext(), "当前设备没有账户信息", Toast.LENGTH_LONG).show();
        }
        View father = inflater.inflate(R.layout.framgent_login_select, container, false);
        onClickSwitchFragment(father.findViewById(R.id.mailLogin), MailLoginFragment.newInstance(user.getEmail()));
        onClickSwitchFragment(father.findViewById(R.id.passwordLogin), PasswordLoginFragment.newInstance(user));
        onClickSwitchFragment(father.findViewById(R.id.keyLogin), KeyLoginFragment.newInstance(user.getEmail()));
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
                    Message message = Message.obtain();
                    message.what = MainActivity.ADD_FRAGMENT;
                    message.obj = fragment;
                    MainActivity.getHandler().sendMessage(message);
                    break;
            }
            return true;
        });
    }
}
