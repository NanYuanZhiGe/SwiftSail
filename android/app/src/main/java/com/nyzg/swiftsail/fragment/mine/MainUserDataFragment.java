package com.nyzg.swiftsail.fragment.mine;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.repository.LoginRepository;

public class MainUserDataFragment extends Fragment {

    public static Fragment getInstance() {
        return new MainUserDataFragment();
    }

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_mine_user_data, container, false);
        LoginRepository.INSTANCE.getMutableCurrentUser().observe(getViewLifecycleOwner(), user -> {
            ((TextView) father.findViewById(R.id.nickName).findViewById(R.id.content)).setText(user.getNickName());
            ((TextView) father.findViewById(R.id.id).findViewById(R.id.content)).setText(user.getId() + "");
            ((TextView) father.findViewById(R.id.email).findViewById(R.id.content)).setText(user.getEmail());
        });
        return father;
    }
}