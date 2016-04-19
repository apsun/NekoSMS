package com.crossbowffs.nekosms.app;

import com.crossbowffs.nekosms.BuildConfig;

public final class BroadcastConsts {
    private static final String PACKAGE_NAME = BuildConfig.APPLICATION_ID;
    public static final String ACTION_RECEIVE_SMS = PACKAGE_NAME + ".action.RECEIVE_BLOCKED_SMS";
    public static final String ACTION_DELETE_SMS = PACKAGE_NAME + ".action.DELETE_BLOCKED_SMS";
    public static final String ACTION_RESTORE_SMS = PACKAGE_NAME + ".action.RESTORE_BLOCKED_SMS";
    public static final String ACTION_DISMISS_NOTIFICATION = PACKAGE_NAME + ".action.DISMISS_NOTIFICATION";
    public static final String EXTRA_MESSAGE = "message";

    private BroadcastConsts() { }
}
