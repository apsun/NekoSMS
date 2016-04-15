package com.crossbowffs.nekosms.remotepreferences;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exposes {@link SharedPreferences} to other apps running on the device.
 *
 * To use, simply extend this class and call the constructor with the
 * appropriate authority and preference name parameters. When accessing
 * the preferences, use {@link RemotePreferences} initialized with the
 * same parameters.
 *
 * For granular access control, override {@link #checkAccess(String, String, boolean)}
 * and return {@code false} to deny the operation.
 */
public abstract class RemotePreferenceProvider extends ContentProvider implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int PREFERENCES_ID = 1;
    private static final int PREFERENCE_ID = 2;

    private final Uri mBaseUri;
    private final String[] mPrefNames;
    private final Map<String, SharedPreferences> mPreferences;
    private final UriMatcher mUriMatcher;

    public RemotePreferenceProvider(String authority, String[] prefNames) {
        mBaseUri = Uri.parse("content://" + authority);
        mPrefNames = prefNames;
        mPreferences = new HashMap<String, SharedPreferences>(prefNames.length);
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        mUriMatcher.addURI(authority, "*/", PREFERENCES_ID);
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
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        PrefKeyPair prefKeyPair = parseUri(uri);
        SharedPreferences preferences = getPreferences(prefKeyPair, false);
        Map<String, ?> preferenceMap = preferences.getAll();
        MatrixCursor cursor = new MatrixCursor(projection);
        if (prefKeyPair.mPrefKey.isEmpty()) {
            for (Map.Entry<String, ?> entry : preferenceMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                cursor.addRow(buildRow(projection, key, value));
            }
        } else {
            String key = prefKeyPair.mPrefKey;
            Object value = preferenceMap.get(key);
            cursor.addRow(buildRow(projection, key, value));
        }
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        PrefKeyPair prefKeyPair = parseUri(uri);
        String key = prefKeyPair.mPrefKey;
        if (key.isEmpty()) {
            key = values.getAsString(RemoteContract.COLUMN_KEY);
        }
        int type = values.getAsInteger(RemoteContract.COLUMN_TYPE);
        Object value = RemoteUtils.deserialize(values.get(RemoteContract.COLUMN_VALUE), type);
        SharedPreferences preferences = getPreferences(prefKeyPair, true);
        SharedPreferences.Editor editor = preferences.edit();
        if (value == null) {
            throw new IllegalArgumentException("Attempting to insert preference with null value");
        } else if (value instanceof String) {
            editor.putString(key, (String)value);
        } else if (value instanceof Set<?>) {
            editor.putStringSet(key, RemoteUtils.toStringSet(value));
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer)value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long)value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float)value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean)value);
        } else {
            throw new IllegalArgumentException("Cannot set preference with type " + value.getClass());
        }
        editor.commit();
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        PrefKeyPair prefKeyPair = parseUri(uri);
        String key = prefKeyPair.mPrefKey;
        SharedPreferences preferences = getPreferences(prefKeyPair, true);
        if (key.isEmpty()) {
            preferences.edit().clear().commit();
        } else {
            preferences.edit().remove(key).commit();
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
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
        int match = mUriMatcher.match(uri);
        if (match != PREFERENCE_ID && match != PREFERENCES_ID) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
        List<String> pathSegments = uri.getPathSegments();
        String prefName = pathSegments.get(0);
        String prefKey = "";
        if (match == PREFERENCE_ID) {
            prefKey = pathSegments.get(1);
        }
        return new PrefKeyPair(prefName, prefKey);
    }

    private int getPrefType(Object value) {
        if (value == null) return RemoteContract.TYPE_NULL;
        if (value instanceof String) return RemoteContract.TYPE_STRING;
        if (value instanceof Set<?>) return RemoteContract.TYPE_STRING_SET;
        if (value instanceof Integer) return RemoteContract.TYPE_INT;
        if (value instanceof Long) return RemoteContract.TYPE_LONG;
        if (value instanceof Float) return RemoteContract.TYPE_FLOAT;
        if (value instanceof Boolean) return RemoteContract.TYPE_BOOLEAN;
        throw new AssertionError("Unknown preference type: " + value.getClass());
    }

    private Object[] buildRow(String[] projection, String key, Object value) {
        Object[] row = new Object[projection.length];
        for (int i = 0; i < row.length; ++i) {
            String col = projection[i];
            if (RemoteContract.COLUMN_KEY.equals(col)) {
                row[i] = key;
            } else if (RemoteContract.COLUMN_TYPE.equals(col)) {
                row[i] = getPrefType(value);
            } else if (RemoteContract.COLUMN_VALUE.equals(col)) {
                row[i] = RemoteUtils.serialize(value);
            } else {
                throw new IllegalArgumentException("Invalid column name: " + col);
            }
        }
        return row;
    }

    private SharedPreferences getPreferences(PrefKeyPair prefKeyPair, boolean write) {
        String prefName = prefKeyPair.mPrefName;
        String prefKey = prefKeyPair.mPrefKey;
        SharedPreferences prefs = mPreferences.get(prefName);
        if (prefs == null) {
            throw new IllegalArgumentException("Unknown preference file name: " + prefName);
        }
        if (!checkAccess(prefName, prefKey, write)) {
            throw new SecurityException("Insufficient permissions to access: " + prefName + "/" + prefKey);
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

    protected boolean checkAccess(String prefName, String prefKey, boolean write) {
        return true;
    }

    private class PrefKeyPair {
        private final String mPrefName;
        private final String mPrefKey;

        private PrefKeyPair(String prefName, String prefKey) {
            mPrefName = prefName;
            mPrefKey = prefKey;
        }
    }
}
