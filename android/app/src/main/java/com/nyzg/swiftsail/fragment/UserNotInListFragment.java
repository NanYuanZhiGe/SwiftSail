package com.nyzg.swiftsail.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.bean.GlobalToast;
import com.nyzg.swiftsail.bean.MatchUtils;
import com.nyzg.swiftsail.dbobj.User;

public class UserNotInListFragment extends Fragment {
    static public Fragment newInstance() {
        return new UserNotInListFragment();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father;
        father = inflater.inflate(R.layout.fragment_user_not_in_list, container, false);
        TextView nextStep = father.findViewById(R.id.nextStep);
        TextInputEditText inputMail = father.findViewById(R.id.inputMail);
        nextStep.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    nextStep.setPressed(true);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    nextStep.setPressed(false);
                    break;
                case MotionEvent.ACTION_UP:
                    nextStep.setPressed(false);
                    String str = inputMail.getText() != null ? inputMail.getText().toString() : null;
                    if (str == null || str.isEmpty() || MatchUtils.isMailAddrIllegal(str)) {
                        GlobalToast.COMMON_TOAST.accept("邮箱格式不合法");
                        break;
                    }
                    Message m = Message.obtain();
                    m.what = MainActivity.ADD_FRAGMENT;
                    User user = new User();
                    user.setEmail(str);
                    m.obj = LoginTypeSelectFragment.newInstance(user);
                    MainActivity.getHandler().sendMessage(m);
            }
            return true;
        });
        return father;
    }
}
