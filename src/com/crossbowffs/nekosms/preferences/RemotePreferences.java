package com.crossbowffs.nekosms.preferences;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import com.crossbowffs.nekosms.provider.PreferenceProviderBase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides a {@link SharedPreferences} compatible API to {@link PreferenceProviderBase}.
 * Note these important differences:
 * <ul>
 *     <li>Preference change listeners are <b>strongly</b> referenced</li>
 *     <li>{@link #getAll()} is unsupported</li>
 *     <li>{@link #getStringSet(String, Set)} is unsupported</li>
 *     <li>{@link Editor#putStringSet(String, Set)} is unsupported</li>
 *     <li>{@link Editor#apply()} is <b>synchronous</b></li>
 * </ul>
 */
public class RemotePreferences implements SharedPreferences {
    private final Context mContext;
    private final Uri mBaseUri;
    private final Map<OnSharedPreferenceChangeListener, PreferenceContentObserver> mContentObservers = new HashMap<>();

    public RemotePreferences(Context context, String authority, String name) {
        mContext = context;
        mBaseUri = Uri.parse("content://" + authority).buildUpon().appendPath(name).build();
    }

    private Object query(String key, Object defaultValue, Class<?> type) {
        Uri uri = mBaseUri.buildUpon().appendPath(key).build();
        try (Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null)) {
            cursor.moveToFirst();
            if (cursor.isNull(0)) {
                return defaultValue;
            } else if (type == null) {
                return this;
            } else if (type == String.class) {
                return cursor.getString(0);
            } else if (type == Boolean.class) {
                return cursor.getInt(0) != 0;
            } else if (type == Integer.class) {
                return cursor.getInt(0);
            } else if (type == Long.class) {
                return cursor.getLong(0);
            } else if (type == Float.class) {
                return cursor.getFloat(0);
            } else {
                throw new AssertionError("Invalid type: " + type.getName());
            }
        }
    }

    @Override
    public Map<String, ?> getAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString(String key, String defValue) {
        return (String)query(key, defValue, String.class);
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(String key, int defValue) {
        return (Integer)query(key, defValue, Integer.class);
    }

    @Override
    public long getLong(String key, long defValue) {
        return (Long)query(key, defValue, Long.class);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return (Float)query(key, defValue, Float.class);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return (Boolean)query(key, defValue, Boolean.class);
    }

    @Override
    public boolean contains(String key) {
        return query(key, null, null) == this;
    }

    @Override
    public Editor edit() {
        return new RemotePreferencesEditor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        PreferenceContentObserver observer = new PreferenceContentObserver(listener);
        mContentObservers.put(listener, observer);
        mContext.getContentResolver().registerContentObserver(mBaseUri, true, observer);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        PreferenceContentObserver observer = mContentObservers.remove(listener);
        mContext.getContentResolver().unregisterContentObserver(observer);
    }

    private class RemotePreferencesEditor implements Editor {
        private final Map<String, ContentValues> mToAdd = new HashMap<>();
        private final Set<String> mToRemove = new HashSet<>();

        @Override
        public Editor putString(String key, String value) {
            ContentValues values = new ContentValues(1);
            values.put(PreferenceProviderBase.COLUMN_VALUE, value);
            mToAdd.put(key, values);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Editor putInt(String key, int value) {
            ContentValues values = new ContentValues(1);
            values.put(PreferenceProviderBase.COLUMN_VALUE, value);
            mToAdd.put(key, values);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            ContentValues values = new ContentValues(1);
            values.put(PreferenceProviderBase.COLUMN_VALUE, value);
            mToAdd.put(key, values);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            ContentValues values = new ContentValues(1);
            values.put(PreferenceProviderBase.COLUMN_VALUE, value);
            mToAdd.put(key, values);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            ContentValues values = new ContentValues(1);
            values.put(PreferenceProviderBase.COLUMN_VALUE, value);
            mToAdd.put(key, values);
            return this;
        }

        @Override
        public Editor remove(String key) {
            mToRemove.add(key);
            return this;
        }

        @Override
        public Editor clear() {
            return remove("");
        }

        @Override
        public boolean commit() {
            for (String key : mToRemove) {
                Uri uri = mBaseUri.buildUpon().appendPath(key).build();
                mContext.getContentResolver().delete(uri, null, null);
            }
            for (Map.Entry<String, ContentValues> entry : mToAdd.entrySet()) {
                Uri uri = mBaseUri.buildUpon().appendPath(entry.getKey()).build();
                mContext.getContentResolver().insert(uri, entry.getValue());
            }
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }

    private class PreferenceContentObserver extends ContentObserver {
        private final OnSharedPreferenceChangeListener mListener;

        public PreferenceContentObserver(OnSharedPreferenceChangeListener listener) {
            super(null);
            mListener = listener;
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            String prefKey = uri.getLastPathSegment();
            mListener.onSharedPreferenceChanged(RemotePreferences.this, prefKey);
        }
    }
}
