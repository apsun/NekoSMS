package com.crossbowffs.nekosms.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.RingtonePreference;
import android.util.AttributeSet;
import com.crossbowffs.nekosms.R;

public class AutoRingtonePreference extends RingtonePreference {
    private final String mNoneSummary;

    public AutoRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AutoRingtonePreference, 0, 0);
        mNoneSummary = a.getString(R.styleable.AutoRingtonePreference_noneSummary);
        a.recycle();
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
