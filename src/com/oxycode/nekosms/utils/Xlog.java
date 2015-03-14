package com.oxycode.nekosms.utils;

import android.util.Log;

public final class Xlog {
    private static final String TAG = "NekoSMS";
    private static final boolean VLOG = true;

    public static void v(String message, Object... args) {
        if (VLOG) {
            Log.v(TAG, String.format(message, args));
        }
    }

    public static void d(String message, Object... args) {
        Log.d(TAG, String.format(message, args));
    }

    public static void i(String message, Object... args) {
        Log.i(TAG, String.format(message, args));
    }

    public static void w(String message, Object... args) {
        Log.w(TAG, String.format(message, args));
    }

    public static void e(String message, Object... args) {
        Log.e(TAG, String.format(message, args));
    }
}
