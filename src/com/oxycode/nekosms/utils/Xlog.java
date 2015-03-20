package com.oxycode.nekosms.utils;

import android.util.Log;

public final class Xlog {
    private static final boolean VLOG = true;
    private static final boolean DLOG = true;
    private static final boolean MERGE = true;
    private static final String MERGED_TAG = "NekoSMS";

    public static void v(String tag, Object message, Object... args) {
        if (MERGE) {
            tag = MERGED_TAG;
        }
        if (VLOG) {
            if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
                Log.v(tag, String.format(message.toString(), args), (Throwable)args[args.length - 1]);
            } else {
                Log.v(tag, String.format(message.toString(), args));
            }
        }
    }

    public static void d(String tag, Object message, Object... args) {
        if (MERGE) {
            tag = MERGED_TAG;
        }
        if (DLOG) {
            if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
                Log.d(tag, String.format(message.toString(), args), (Throwable)args[args.length - 1]);
            } else {
                Log.d(tag, String.format(message.toString(), args));
            }
        }
    }

    public static void i(String tag, Object message, Object... args) {
        if (MERGE) {
            tag = MERGED_TAG;
        }
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            Log.i(tag, String.format(message.toString(), args), (Throwable)args[args.length - 1]);
        } else {
            Log.i(tag, String.format(message.toString(), args));
        }
    }

    public static void w(String tag, Object message, Object... args) {
        if (MERGE) {
            tag = MERGED_TAG;
        }
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            Log.w(tag, String.format(message.toString(), args), (Throwable)args[args.length - 1]);
        } else {
            Log.w(tag, String.format(message.toString(), args));
        }
    }

    public static void e(String tag, Object message, Object... args) {
        if (MERGE) {
            tag = MERGED_TAG;
        }
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            Log.e(tag, String.format(message.toString(), args), (Throwable)args[args.length - 1]);
        } else {
            Log.e(tag, String.format(message.toString(), args));
        }
    }
}
