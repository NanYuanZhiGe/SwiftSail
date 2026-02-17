package com.nyzg.swiftsail.view.record;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.nyzg.swiftsail.R;

public class StageLayout extends ConstraintLayout {
    private Runnable mCancel = () -> {
    };
    private Runnable mStart = () -> {
    };
    private Runnable mStop = () -> {
    };
    private Runnable mSubmit = () -> {
    };
    private boolean isStop = true;

    public StageLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.layout_stage, this, true);
        View cancel = findViewById(R.id.cancel);
        View pauseOrResume = findViewById(R.id.pauseOrResume);
        View submit = findViewById(R.id.submit);
        cancel.setOnClickListener(v -> mCancel.run());
        pauseOrResume.setOnClickListener(v -> {
            if (isStop) {
                mStart.run();
            } else {
                mStop.run();
            }
            isStop = !isStop;
        });
        submit.setOnClickListener(v -> mSubmit.run());
    }

    public void setMCancel(@NonNull Runnable cancel) {
        mCancel = cancel;
    }

    public void setMStart(@NonNull Runnable start) {
        mStart = start;
    }

    public void setMStop(@NonNull Runnable stop) {
        mStop = stop;
    }

    public void setMSubmit(@NonNull Runnable submit) {
        mSubmit = submit;
    }
}
