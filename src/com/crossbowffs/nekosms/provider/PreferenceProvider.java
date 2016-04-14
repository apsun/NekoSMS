package com.crossbowffs.nekosms.provider;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exposes {@link SharedPreferences} to other apps running on the device.
 */
public abstract class PreferenceProvider extends ContentProvider implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String COLUMN_VALUE = "value";
    private static final int PREFERENCE_ID = 1;

    private final Uri mBaseUri;
    private final String[] mPrefNames;
    private final Map<String, SharedPreferences> mPreferences;
    private final UriMatcher mUriMatcher;

    public PreferenceProvider(String authority, String[] prefNames) {
        mBaseUri = Uri.parse("content://" + authority);
        mPrefNames = prefNames;
        mPreferences = new HashMap<>(prefNames.length);
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(authority, "*/*", PREFERENCE_ID);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        for (String prefName : mPrefNames) {
            SharedPreferences preferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
            preferences.registerOnSharedPreferenceChangeListener(this);
            mPreferences.put(prefName, preferences);
        }
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        PrefKeyPair prefKeyPair = parseUri(uri);
        SharedPreferences preferences = getPreferences(prefKeyPair.mPrefName, false);
        Map<String, ?> preferenceMap = preferences.getAll();
        MatrixCursor cursor = new MatrixCursor(new String[] {COLUMN_VALUE});
        Object value = preferenceMap.get(prefKeyPair.mPrefKey);
        if (isSupprtedType(value)) {
            if (value instanceof Boolean) {
                value = (Boolean)value ? 1 : 0;
            }
            cursor.addRow(new Object[] {value});
        } else {
            throw new IllegalArgumentException("Cannot get preference with value type " + value.getClass().getName());
        }
        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        PrefKeyPair prefKeyPair = parseUri(uri);
        SharedPreferences preferences = getPreferences(prefKeyPair.mPrefName, true);
        String key = prefKeyPair.mPrefKey;
        Object value = values.get(COLUMN_VALUE);
        SharedPreferences.Editor editor = preferences.edit();
        if (value == null) {
            throw new IllegalArgumentException("Attempting to insert preference with null value");
        } else if (value instanceof String) {
            editor.putString(key, (String)value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean)value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer)value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long)value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float)value);
        } else {
            throw new IllegalArgumentException("Cannot set preference with value type " + value.getClass().getName());
        }
        editor.apply();
        return uri;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        PrefKeyPair prefKeyPair = parseUri(uri);
        SharedPreferences preferences = getPreferences(prefKeyPair.mPrefName, true);
        String key = prefKeyPair.mPrefKey;
        if (key.isEmpty()) {
            preferences.edit().clear().apply();
        } else {
            preferences.edit().remove(key).apply();
        }
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        insert(uri, values);
        return 0;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String prefName = getPreferencesName(sharedPreferences);
        Uri uri = mBaseUri.buildUpon().appendPath(prefName).appendPath(key).build();
        getContext().getContentResolver().notifyChange(uri, null);
    }

    private PrefKeyPair parseUri(Uri uri) {
        if (mUriMatcher.match(uri) != PREFERENCE_ID) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
        List<String> pathSegments = uri.getPathSegments();
        String prefName = pathSegments.get(0);
        String prefKey = pathSegments.get(1);
        return new PrefKeyPair(prefName, prefKey);
    }

    private boolean isSupprtedType(Object value) {
        if (value == null) return true;
        if (value instanceof String) return true;
        if (value instanceof Boolean) return true;
        if (value instanceof Integer) return true;
        if (value instanceof Long) return true;
        if (value instanceof Float) return true;
        return false;
    }

    private SharedPreferences getPreferences(String prefName, boolean write) {
        if (!checkAccess(prefName, write)) {
            throw new SecurityException("Insufficient permissions to access: " + prefName);
        }
        SharedPreferences prefs = mPreferences.get(prefName);
        if (prefs == null) {
            throw new AssertionError("Preference not provided in constructor: " + prefName);
        }
        return prefs;
    }

    private String getPreferencesName(SharedPreferences preferences) {
        for (Map.Entry<String, SharedPreferences> entry : mPreferences.entrySet()) {
            if (entry.getValue() == preferences) {
                return entry.getKey();
            }
        }
        throw new AssertionError("Cannot find name for SharedPreferences");
    }

    protected abstract boolean checkAccess(String prefName, boolean write);

    private class PrefKeyPair {
        public final String mPrefName;
        public final String mPrefKey;

        public PrefKeyPair(String prefName, String prefKey) {
            mPrefName = prefName;
            mPrefKey = prefKey;
        }
    }
}
