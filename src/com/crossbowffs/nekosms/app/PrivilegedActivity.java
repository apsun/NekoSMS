package com.crossbowffs.nekosms.app;

import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class PrivilegedActivity extends AppCompatActivity {
    public interface PrivilegedActionCallback {
        void run(boolean granted);
    }

    private final Map<Integer, PrivilegedActionCallback> mActionCallbacks;

    public PrivilegedActivity() {
        mActionCallbacks = new HashMap<>(2);
    }

    public void runPrivilegedAction(String[] permissions, int requestCode, PrivilegedActionCallback callback) {
        for (String permission : permissions) {
            int permissionStatus = ContextCompat.checkSelfPermission(this, permission);
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                mActionCallbacks.put(requestCode, callback);
                ActivityCompat.requestPermissions(this, permissions, requestCode);
                return;
            }
        }

        callback.run(true);
    }

    public void runPrivilegedAction(String permission, int requestCode, PrivilegedActionCallback callback) {
        int permissionStatus = ContextCompat.checkSelfPermission(this, permission);
        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            mActionCallbacks.put(requestCode, callback);
            ActivityCompat.requestPermissions(this, new String[] {permission}, requestCode);
        } else {
            callback.run(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        PrivilegedActionCallback callback = mActionCallbacks.remove(requestCode);
        if (callback == null) {
            return;
        }

        for (int permissionStatus : grantResults) {
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                callback.run(false);
                return;
            }
        }

        callback.run(true);
    }
}
