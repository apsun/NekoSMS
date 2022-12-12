package com.crossbowffs.nekosms.app;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class NekoSmsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Enroll in dynamic colors if available; otherwise fall back to custom
        // theme specified in resources
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
