package com.crossbowffs.nekosms.widget;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.util.AttributeSet;

// TODO: This is broken, need to port RingtonePreference to androidx.preference.Preference
public class AutoRingtonePreference extends RingtonePreference {
    private final CharSequence mNoneSummary;

    public AutoRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mNoneSummary = getSummary();
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        super.onSaveRingtone(ringtoneUri);
        Ringtone ringtone = null;
        if (ringtoneUri != null) {
            ringtone = RingtoneManager.getRingtone(getContext(), ringtoneUri);
        }
        if (ringtone == null) {
            setSummary(mNoneSummary);
        } else {
            String name = ringtone.getTitle(getContext());
            setSummary(name);
        }
    }

    @Override
    protected void onAttachedToActivity() {
        super.onAttachedToActivity();
        onSaveRingtone(onRestoreRingtone());
    }
}
