package com.crossbowffs.nekosms.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

public final class PermissionUtils {
    private PermissionUtils() { }

    public static boolean checkPermissions(Context context, String[] permissions, int[] outGrantResults) {
        boolean allGranted = true;
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int permissionStatus = ContextCompat.checkSelfPermission(context, permission);
            if (outGrantResults != null) {
                outGrantResults[i] = permissionStatus;
            }
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
            }
        }
        return allGranted;
    }
}
