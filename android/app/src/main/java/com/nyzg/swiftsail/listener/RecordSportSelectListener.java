package com.nyzg.swiftsail.listener;

import android.view.MotionEvent;
import android.view.View;

import com.nyzg.swiftsail.viewmodel.RecordSelectViewModel;

public class RecordSportSelectListener implements View.OnTouchListener {
    final private RecordSelectViewModel recordSelectViewModel;
    final private String type;

    public RecordSportSelectListener(RecordSelectViewModel recordSelectViewModel, String type) {
        this.recordSelectViewModel = recordSelectViewModel;
        this.type = type;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (recordSelectViewModel == null) {
            return true;
        }
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                view.setPressed(true);
                break;
            case MotionEvent.ACTION_CANCEL:
                view.setPressed(false);
                break;
            case MotionEvent.ACTION_UP:
                view.setPressed(false);
                recordSelectViewModel.changeFragment(type);
                break;
        }
        return true;
    }
}
