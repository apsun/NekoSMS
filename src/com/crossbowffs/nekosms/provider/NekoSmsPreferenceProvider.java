package com.crossbowffs.nekosms.provider;

import com.crossbowffs.nekosms.preferences.PrefConsts;
import com.crossbowffs.nekosms.remotepreferences.RemotePreferenceProvider;

public class NekoSmsPreferenceProvider extends RemotePreferenceProvider {
    public NekoSmsPreferenceProvider() {
        super(PrefConsts.REMOTE_PREFS_AUTHORITY, new String[] {PrefConsts.FILE_MAIN});
    }

    @Override
    protected boolean checkAccess(String prefName, String prefKey, boolean write) {
        // Only allow read access
        if (write) {
            return false;
        }

        // Only allow access to enable preference
        if (!PrefConsts.KEY_ENABLE.equals(prefKey)) {
            return false;
        }

        // Only allow access from telephony process
        String callerPackage = getCallingPackage();
        return "com.android.phone".equals(callerPackage);
    }
}
