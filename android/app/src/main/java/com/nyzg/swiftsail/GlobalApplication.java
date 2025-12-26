package com.nyzg.swiftsail;

import android.app.Application;
import android.content.Context;

import org.maplibre.android.MapLibre;
import org.maplibre.android.maps.MapView;


public class GlobalApplication extends Application {
    private static GlobalApplication self;

    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
        initMapBox();
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
