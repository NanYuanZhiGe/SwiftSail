package com.nyzg.swiftsail;

import android.app.Application;
import android.content.Context;

public class GlobalApplication extends Application {
    private static GlobalApplication self;

    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
    }

    public static GlobalApplication getInstance() {
        return self;
    }

    public static Context getAppContext() {
        return self.getApplicationContext();
    }
}
