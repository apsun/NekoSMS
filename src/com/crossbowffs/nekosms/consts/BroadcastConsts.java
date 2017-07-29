package com.crossbowffs.nekosms.consts;

import com.crossbowffs.nekosms.BuildConfig;

public final class BroadcastConsts {
    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;
    public static final String RECEIVER_NAME = NEKOSMS_PACKAGE + ".app.BlockedSmsReceiver";
    public static final String ACTION_RECEIVE_SMS = NEKOSMS_PACKAGE + ".action.RECEIVE_BLOCKED_SMS";
    public static final String ACTION_DELETE_SMS = NEKOSMS_PACKAGE + ".action.DELETE_BLOCKED_SMS";
    public static final String ACTION_RESTORE_SMS = NEKOSMS_PACKAGE + ".action.RESTORE_BLOCKED_SMS";
    public static final String ACTION_DISMISS_NOTIFICATION = NEKOSMS_PACKAGE + ".action.DISMISS_NOTIFICATION";
    public static final String EXTRA_MESSAGE = "message";

    private BroadcastConsts() { }
}
