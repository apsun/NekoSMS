package com.crossbowffs.nekosms.provider;

import android.os.Binder;
import com.crossbowffs.nekosms.preferences.PrefKeys;

public class PreferenceProviderImpl extends PreferenceProvider {
    public PreferenceProviderImpl() {
        super(PrefKeys.REMOTE_PREFS_AUTHORITY, new String[] {PrefKeys.FILE_MAIN});
    }

    @Override
    protected boolean checkAccess(String prefName, boolean write) {
        // Only allow read access
        if (write) {
            return false;
        }

        // Only allow access to the default shared preferences
        if (!PrefKeys.FILE_MAIN.equals(prefName)) {
            return false;
        }

        // Only allow reads from telephony process
        int callerUid = Binder.getCallingUid();
        String callerName = getContext().getPackageManager().getNameForUid(callerUid);
        String uidName = callerName.split(":")[0];
        return "android.uid.phone".equals(uidName);
    }
}
