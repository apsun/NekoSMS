package com.crossbowffs.nekosms.app;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import com.crossbowffs.nekosms.R;

public class RingtonePreference extends Preference {
    public RingtonePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public RingtonePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RingtonePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RingtonePreference(@NonNull Context context) {
        super(context);
    }

    private String getRingtoneName(Uri ringtoneUri) {
        Context context = getContext();
        if (ringtoneUri == null) {
            return context.getString(R.string.pref_notifications_ringtone_silent);
        }

        Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
        if (ringtone == null) {
            return context.getString(R.string.pref_notifications_ringtone_unknown);
        } else {
            return ringtone.getTitle(context);
        }
    }

    private void saveRingtoneUri(Uri ringtoneUri) {
        persistString(ringtoneUriToString(ringtoneUri));
        setSummary(getRingtoneName(ringtoneUri));
    }

    private String ringtoneUriToString(Uri ringtoneUri) {
        if (ringtoneUri == null) {
            return "";
        } else {
            return ringtoneUri.toString();
        }
    }

    private Uri stringToRingtoneUri(String str) {
        if (str == null) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else if (str.isEmpty()) {
            return null;
        } else {
            return Uri.parse(str);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        String value = getPersistedString((String)defaultValue);
        saveRingtoneUri(stringToRingtoneUri(value));
    }

    public Intent getRingtonePickerIntent() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getTitle());
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            stringToRingtoneUri(getPersistedString(null)));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        return intent;
    }

    public boolean onRingtonePickerResult(Intent data) {
        if (data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (callChangeListener(ringtoneUriToString(uri))) {
                saveRingtoneUri(uri);
            }
        }
        return true;
    }
}
