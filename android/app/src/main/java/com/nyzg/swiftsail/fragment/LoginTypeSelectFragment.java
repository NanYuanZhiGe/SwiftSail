package com.nyzg.swiftsail.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.R;


@SuppressLint("ClickableViewAccessibility")
public class LoginTypeSelectFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.framgent_login_select, container, false);
        onClickSwitchFragment(father.findViewById(R.id.mailLogin), new MailLoginFragment());
        onClickSwitchFragment(father.findViewById(R.id.passwordLogin), new PasswordLoginFragment());
        onClickSwitchFragment(father.findViewById(R.id.keyLogin), new KeyLoginFragment());
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
