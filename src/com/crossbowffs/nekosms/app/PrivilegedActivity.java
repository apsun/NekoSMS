package com.crossbowffs.nekosms.app;

import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

/* package */ class PrivilegedActivity extends AppCompatActivity {
    public void runPrivilegedAction(int requestCode, String... permissions) {
        for (String permission : permissions) {
            int permissionStatus = ContextCompat.checkSelfPermission(this, permission);
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, requestCode);
                return;
            }
        }

        onRequestPermissionsResult(requestCode, true);
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int permissionStatus : grantResults) {
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                onRequestPermissionsResult(requestCode, false);
                return;
            }
        }

        // Empty permissions means that the request was cancelled
        // by the user, so the permissions were not granted
        boolean requestCancelled = (grantResults.length == 0);
        onRequestPermissionsResult(requestCode, !requestCancelled);
    }

    public void onRequestPermissionsResult(int requestCode, boolean granted) {

    }
}
