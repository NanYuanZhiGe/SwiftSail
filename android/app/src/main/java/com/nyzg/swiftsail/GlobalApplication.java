package com.nyzg.swiftsail;

import android.app.Application;
import android.content.Context;

import com.github.boybeak.skbglobal.SoftKeyboardGlobal;

import org.maplibre.android.MapLibre;


public class GlobalApplication extends Application {
    private static GlobalApplication self;

    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
        initMapBox();
        SoftKeyboardGlobal.INSTANCE.install(this,false);
    }

    private void initMapBox() {
        MapLibre.getInstance(this);
    }

    public static GlobalApplication getInstance() {
        return self;
    }

    public static Context getAppContext() {
        return self.getApplicationContext();
    }
}
