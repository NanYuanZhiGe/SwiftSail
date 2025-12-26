package com.nyzg.swiftsail.listener;

import android.content.Context;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;

import com.nyzg.swiftsail.RecordActivity;


public class RecordBtnListener implements View.OnTouchListener {
    private final Context context;

    public RecordBtnListener(Context context){
        this.context=context;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                view.setPressed(true);
                break;
            case MotionEvent.ACTION_CANCEL:
                view.setPressed(false);
                break;
            case MotionEvent.ACTION_UP:
                view.setPressed(false);
                Intent intent=new Intent(context, RecordActivity.class);
                context.startActivity(intent);
        }
        return true;
    }
}
