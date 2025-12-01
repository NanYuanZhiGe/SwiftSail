package com.nyzg.swiftsail.adapter;

import android.annotation.SuppressLint;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nyzg.swiftsail.MainActivity;
import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.dbobj.User;
import com.nyzg.swiftsail.fragment.LoginTypeSelectFragment;
import com.nyzg.swiftsail.fragment.UserNotInListFragment;

import java.util.ArrayList;
import java.util.List;

public class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.MyViewHolder> {
    List<User> userList;

    public AccountListAdapter(List<User> userList) {
        if (userList == null) {
            this.userList = new ArrayList<>(1);
        } else {
            this.userList = userList;
        }
        User user = new User();
        user.setId(-1);
        this.userList.add(user);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new MyViewHolder(inflater.inflate(R.layout.account_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.bind(userList.get(position));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView nickName;
        TextView email;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nickName = itemView.findViewById(R.id.accountNickName);
            email = itemView.findViewById(R.id.accountEmail);
        }

        @SuppressLint("ClickableViewAccessibility")
        public void bind(User user) {
            if (user.getId() == -1) {
                userNotInList();
                return;
            }
            nickName.setText(user.getNickName());
            email.setText(user.getEmail());
            itemView.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    itemView.setPressed(true);
                    return true;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                    itemView.setPressed(false);
                    return true;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    itemView.setPressed(false);
                    Message message = Message.obtain();
                    message.what = MainActivity.ADD_FRAGMENT;
                    message.obj = LoginTypeSelectFragment.newInstance(user);
                    MainActivity.getHandler().sendMessage(message);
                    return true;
                }
                return true;
            });
        }

        @SuppressLint("ClickableViewAccessibility")
        private void userNotInList() {
            nickName.setText("账户未列出？");
            email.setText("");
            itemView.setOnTouchListener((view, motionEvent) -> {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        itemView.setPressed(true);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        itemView.setPressed(false);
                        break;
                    case MotionEvent.ACTION_UP:
                        itemView.setPressed(false);
                        Message m = Message.obtain();
                        m.what = MainActivity.ADD_FRAGMENT;
                        m.obj = UserNotInListFragment.newInstance();
                        MainActivity.getHandler().sendMessage(m);
                }
                return true;
            });
        }
    }
}
