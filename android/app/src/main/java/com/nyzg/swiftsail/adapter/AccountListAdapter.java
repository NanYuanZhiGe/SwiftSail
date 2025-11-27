package com.nyzg.swiftsail.adapter;

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

import java.util.ArrayList;
import java.util.List;

public class AccountListAdapter extends RecyclerView.Adapter<AccountListAdapter.MyViewHolder> {
    List<User> userList;

    public AccountListAdapter(List<User> userList) {
        if (userList == null) {
            this.userList = new ArrayList<>(0);
            return;
        }
        this.userList = userList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new MyViewHolder(inflater.inflate(R.layout.account_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        User user = userList.get(position);
        holder.setNickName(user.getNickName());
        holder.setEmail(user.getEmail());
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
            itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
                        itemView.setPressed(true);
                        return true;
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                        itemView.setPressed(false);
                        Message message = Message.obtain();
                        message.what = MainActivity.ADD_FRAGMENT;
                        message.obj = new LoginTypeSelectFragment();
                        MainActivity.getHandler().sendMessage(message);
                        return true;
                    }
                    return true;
                }
            });
        }

        public void setNickName(String nickName) {
            this.nickName.setText(nickName);
        }

        public void setEmail(String email) {
            this.email.setText(email);
        }
    }
}
