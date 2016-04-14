package com.crossbowffs.nekosms.provider;

import android.os.Binder;
import com.crossbowffs.nekosms.preferences.PrefKeys;

public class MainPreferenceProvider extends PreferenceProviderBase {
    public MainPreferenceProvider() {
        super(PrefKeys.REMOTE_PREFS_AUTHORITY, new String[] {PrefKeys.FILE_MAIN});
    }

    @Override
    protected boolean checkAccess(String prefName, String prefKey, boolean write) {
        // Only allow read access
        if (write) {
            return false;
        }

        // Only allow access to enable preference
        if (!PrefKeys.KEY_ENABLE.equals(prefKey)) {
            return false;
        }

        // Only allow access from telephony process
        int callerUid = Binder.getCallingUid();
        String callerName = getContext().getPackageManager().getNameForUid(callerUid);
        String uidName = callerName.split(":")[0];
        return "android.uid.phone".equals(uidName);
    }
}
