package com.crossbowffs.nekosms.utils;

import android.util.Log;

public final class Xlog {
    private static final boolean VLOG = true;
    private static final boolean DLOG = true;
    private static final boolean MERGE = true;
    private static final String MERGED_TAG = "NekoSMS";

    private Xlog() { }

    private static void log(int priority, String tag, String message, Object... args) {
        if (MERGE) {
            tag = MERGED_TAG;
        }

        message = String.format(message, args);
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            Throwable throwable = (Throwable)args[args.length - 1];
            String stacktraceStr = Log.getStackTraceString(throwable);
            message = message + '\n' + stacktraceStr;
        }

        Log.println(priority, tag, message);
    }

    public static void v(String tag, String message, Object... args) {
        if (VLOG) {
            log(Log.VERBOSE, tag, message, args);
        }
    }

    public static void d(String tag, String message, Object... args) {
        if (DLOG) {
            log(Log.DEBUG, tag, message, args);
        }
    }

    public static void i(String tag, String message, Object... args) {
        log(Log.INFO, tag, message, args);
    }

    public static void w(String tag, String message, Object... args) {
        log(Log.WARN, tag, message, args);
    }

    public static void e(String tag, String message, Object... args) {
        log(Log.ERROR, tag, message, args);
    }
}
