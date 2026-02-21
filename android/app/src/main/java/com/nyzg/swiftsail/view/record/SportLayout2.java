package com.nyzg.swiftsail.view.record;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nyzg.swiftsail.R;
import com.nyzg.swiftsail.repository.RecordRecordRepository;
import com.nyzg.swiftsail.view.SportLayout;

public class SportLayout2 extends SportLayout {
    public SportLayout2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setOnClickListener(v -> {
            RecordRecordRepository.getInstance().SELECT_TYPE.setValue(SportLayout2.this.getType());;
            RecordRecordRepository.getInstance().SELECT_NAME.setValue(SportLayout2.this.getName());
        });
    }

    @Override
    protected void initLayout(@NonNull Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_sport_2, this, true);
    }
}
