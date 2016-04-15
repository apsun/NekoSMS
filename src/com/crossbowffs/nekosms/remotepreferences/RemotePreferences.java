package com.crossbowffs.nekosms.remotepreferences;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Provides a {@link SharedPreferences} compatible API to
 * {@link RemotePreferenceProvider}. See {@link RemotePreferenceProvider}
 * for more information.
 */
public class RemotePreferences implements SharedPreferences {
    private final Context mContext;
    private final Handler mHandler;
    private final Uri mBaseUri;
    private final WeakHashMap<OnSharedPreferenceChangeListener, PreferenceContentObserver> mListeners;

    public RemotePreferences(Context context, String authority, String prefName) {
        mContext = context;
        mHandler = new Handler(context.getMainLooper());
        mBaseUri = Uri.parse("content://" + authority).buildUpon().appendPath(prefName).build();
        mListeners = new WeakHashMap<OnSharedPreferenceChangeListener, PreferenceContentObserver>();
    }

    @Override
    public Map<String, ?> getAll() {
        return queryAll();
    }

    @Override
    public String getString(String key, String defValue) {
        return (String)querySingle(key, defValue, RemoteContract.TYPE_STRING);
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        return RemoteUtils.toStringSet(querySingle(key, defValues, RemoteContract.TYPE_STRING_SET));
    }

    @Override
    public int getInt(String key, int defValue) {
        return (Integer)querySingle(key, defValue, RemoteContract.TYPE_INT);
    }

    @Override
    public long getLong(String key, long defValue) {
        return (Long)querySingle(key, defValue, RemoteContract.TYPE_LONG);
    }

    @Override
    public float getFloat(String key, float defValue) {
        return (Float)querySingle(key, defValue, RemoteContract.TYPE_FLOAT);
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return (Boolean)querySingle(key, defValue, RemoteContract.TYPE_BOOLEAN);
    }

    @Override
    public boolean contains(String key) {
        return containsKey(key);
    }

    @Override
    public Editor edit() {
        return new RemotePreferencesEditor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (mListeners.containsKey(listener)) return;
        PreferenceContentObserver observer = new PreferenceContentObserver(listener);
        mListeners.put(listener, observer);
        mContext.getContentResolver().registerContentObserver(mBaseUri, true, observer);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        PreferenceContentObserver observer = mListeners.remove(listener);
        if (observer != null) {
            mContext.getContentResolver().unregisterContentObserver(observer);
        }
    }

    private Object querySingle(String key, Object defValue, int expectedType) {
        Uri uri = mBaseUri.buildUpon().appendPath(key).build();
        String[] columns = {RemoteContract.COLUMN_TYPE, RemoteContract.COLUMN_VALUE};
        Cursor cursor = mContext.getContentResolver().query(uri, columns, null, null, null);
        try {
            if (cursor == null || !cursor.moveToFirst() || cursor.getInt(0) == RemoteContract.TYPE_NULL) {
                return defValue;
            } else if (cursor.getInt(0) != expectedType) {
                throw new ClassCastException("Preference type mismatch");
            } else {
                return getValue(cursor, 0, 1);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Map<String, Object> queryAll() {
        Uri uri = mBaseUri.buildUpon().appendPath("").build();
        String[] columns = {RemoteContract.COLUMN_KEY, RemoteContract.COLUMN_TYPE, RemoteContract.COLUMN_VALUE};
        Cursor cursor = mContext.getContentResolver().query(uri, columns, null, null, null);
        try {
            HashMap<String, Object> map = new HashMap<String, Object>(0);
            if (cursor == null) {
                return map;
            }
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                map.put(name, getValue(cursor, 1, 2));
            }
            return map;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private boolean containsKey(String key) {
        Uri uri = mBaseUri.buildUpon().appendPath(key).build();
        String[] columns = {RemoteContract.COLUMN_TYPE};
        Cursor cursor = mContext.getContentResolver().query(uri, columns, null, null, null);
        try {
            return (cursor != null && cursor.moveToFirst() && cursor.getInt(0) != RemoteContract.TYPE_NULL);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private Object getValue(Cursor cursor, int typeCol, int valueCol) {
        int expectedType = cursor.getInt(typeCol);
        switch (expectedType) {
        case RemoteContract.TYPE_STRING:
            return cursor.getString(valueCol);
        case RemoteContract.TYPE_STRING_SET:
            return RemoteUtils.deserializeStringSet(cursor.getString(valueCol));
        case RemoteContract.TYPE_INT:
            return cursor.getInt(valueCol);
        case RemoteContract.TYPE_LONG:
            return cursor.getLong(valueCol);
        case RemoteContract.TYPE_FLOAT:
            return cursor.getFloat(valueCol);
        case RemoteContract.TYPE_BOOLEAN:
            return cursor.getInt(valueCol) != 0;
        default:
            throw new AssertionError("Invalid expected type: " + expectedType);
        }
    }

    private class RemotePreferencesEditor implements Editor {
        private final List<ContentValues> mToAdd = new ArrayList<ContentValues>();
        private final Set<String> mToRemove = new HashSet<String>();

        private ContentValues add(String key, int type) {
            ContentValues values = new ContentValues(3);
            values.put(RemoteContract.COLUMN_KEY, key);
            values.put(RemoteContract.COLUMN_TYPE, type);
            mToAdd.add(values);
            return values;
        }

        @Override
        public Editor putString(String key, String value) {
            add(key, RemoteContract.TYPE_STRING)
                .put(RemoteContract.COLUMN_VALUE, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> value) {
            add(key, RemoteContract.TYPE_STRING_SET)
                .put(RemoteContract.COLUMN_VALUE, RemoteUtils.serializeStringSet(value));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            add(key, RemoteContract.TYPE_INT)
                .put(RemoteContract.COLUMN_VALUE, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            add(key, RemoteContract.TYPE_LONG)
                .put(RemoteContract.COLUMN_VALUE, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            add(key, RemoteContract.TYPE_FLOAT)
                .put(RemoteContract.COLUMN_VALUE, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            add(key, RemoteContract.TYPE_BOOLEAN)
                .put(RemoteContract.COLUMN_VALUE, value ? 1 : 0);
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
            ContentValues[] values = mToAdd.toArray(new ContentValues[mToAdd.size()]);
            Uri uri = mBaseUri.buildUpon().appendPath("").build();
            mContext.getContentResolver().bulkInsert(uri, values);
            return true;
        }

        @Override
        public void apply() {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    commit();
                    return null;
                }
            }.execute();
        }
    }

    private class PreferenceContentObserver extends ContentObserver {
        private final WeakReference<OnSharedPreferenceChangeListener> mListener;

        private PreferenceContentObserver(OnSharedPreferenceChangeListener listener) {
            super(mHandler);
            mListener = new WeakReference<OnSharedPreferenceChangeListener>(listener);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            String prefKey = uri.getLastPathSegment();
            OnSharedPreferenceChangeListener listener = mListener.get();
            if (listener == null) {
                mContext.getContentResolver().unregisterContentObserver(this);
            } else {
                listener.onSharedPreferenceChanged(RemotePreferences.this, prefKey);
            }
        }
    }
}
