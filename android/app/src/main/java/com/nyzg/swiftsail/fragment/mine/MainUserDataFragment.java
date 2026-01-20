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
import com.nyzg.swiftsail.fragment.BackPressQuitFragment;
import com.nyzg.swiftsail.repository.LoginRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MainUserDataFragment extends BackPressQuitFragment {

    public static Fragment getInstance() {
        return new MainUserDataFragment();
    }

    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View father = inflater.inflate(R.layout.fragment_mine_user_data, container, false);
        LoginRepository.getInstance().getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            ((TextView) father.findViewById(R.id.nickName).findViewById(R.id.content)).setText(user.nickName);
            ((TextView) father.findViewById(R.id.id).findViewById(R.id.content)).setText(user.id + "");
            ((TextView) father.findViewById(R.id.email).findViewById(R.id.content)).setText(user.email);
            ((TextView) father.findViewById(R.id.createTime).findViewById(R.id.content)).setText(LocalDate.ofEpochDay(user.createTime).format(DATE_TIME_FORMATTER));
        });
        return father;
    }
}