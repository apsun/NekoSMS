package com.crossbowffs.nekosms.provider;

import com.crossbowffs.nekosms.app.PreferenceConsts;
import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

public class PreferenceProvider extends RemotePreferenceProvider {
    public PreferenceProvider() {
        super(PreferenceConsts.REMOTE_PREFS_AUTHORITY, new String[] {PreferenceConsts.FILE_MAIN});
    }

    @Override
    protected boolean checkAccess(String prefName, String prefKey, boolean write) {
        // Only allow read access
        if (write) {
            return false;
        }

        // Only allow access to main (user) preferences
        if (!PreferenceConsts.FILE_MAIN.equals(prefName)) {
            return false;
        }

        // Only allow access to enable preference
        if (!PreferenceConsts.KEY_ENABLE.equals(prefKey)) {
            return false;
        }

        // Only allow access from telephony process
        String callerPackage = getCallingPackage();
        return "com.android.phone".equals(callerPackage);
    }
}
