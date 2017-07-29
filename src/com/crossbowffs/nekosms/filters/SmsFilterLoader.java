package com.crossbowffs.nekosms.filters;

import android.content.*;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.loader.CursorWrapper;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.Xlog;

public class SmsFilterLoader {
    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;

    private final Context mContext;
    private final ContentObserver mContentObserver;
    private final BroadcastReceiver mBroadcastReceiver;
    private SmsFilter[] mCachedFilters;

    public SmsFilterLoader(Context context) {
        mContext = context;
        mContentObserver = registerContentObserver();
        mBroadcastReceiver = registerBroadcastReceiver();
    }

    public void close() {
        unregisterContentObserver(mContentObserver);
        unregisterBroadcastReceiver(mBroadcastReceiver);
    }

    public SmsFilter[] getFilters() {
        SmsFilter[] filters = mCachedFilters;
        if (filters == null) {
            Xlog.i("Cached SMS filters dirty, loading from database");
            filters = mCachedFilters = loadFilters();
        }
        return filters;
    }

    private void invalidateCache() {
        mCachedFilters = null;
    }

    private SmsFilter[] loadFilters() {
        try (CursorWrapper<SmsFilterData> filterCursor = FilterRuleLoader.get().queryAll(mContext)) {
            if (filterCursor == null) {
                // This might occur if the app has been uninstalled (removing the DB),
                // but the user has not rebooted their device yet. We should not filter
                // any messages in this state.
                Xlog.e("Failed to load SMS filters (queryAll returned null)");
                return null;
            }

            // Whitelist rules come before blacklist rules. Use a front and
            // back array "cursor" to separate the two sections.
            SmsFilter[] filters = new SmsFilter[filterCursor.getCount()];
            int whitelistIndex = 0;
            int blacklistIndex = filters.length;

            SmsFilterData data = new SmsFilterData();
            while (filterCursor.moveToNext()) {
                SmsFilter filter;
                try {
                    filter = new SmsFilter(filterCursor.get(data));
                } catch (Exception e) {
                    Xlog.e("Failed to load SMS filter", e);
                    continue;
                }

                if (data.getAction() == SmsFilterAction.BLOCK) {
                    filters[--blacklistIndex] = filter;
                } else if (data.getAction() == SmsFilterAction.ALLOW) {
                    filters[whitelistIndex++] = filter;
                }
            }

            if (whitelistIndex != blacklistIndex) {
                throw new AssertionError("Whitelist and blacklist cursor position mismatch");
            }

            return filters;
        }
    }

    private ContentObserver registerContentObserver() {
        Xlog.i("Registering SMS filter content observer");

        ContentObserver contentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                Xlog.i("SMS filter database updated, marking cache as dirty");
                invalidateCache();
            }
        };

        ContentResolver contentResolver = mContext.getContentResolver();
        contentResolver.registerContentObserver(DatabaseContract.FilterRules.CONTENT_URI, true, contentObserver);
        return contentObserver;
    }

    private BroadcastReceiver registerBroadcastReceiver() {
        // It is necessary to listen for these events because uninstalling
        // an app or clearing its data does not notify registered ContentObservers.
        // If the filter cache is not cleared, messages may be unintentionally blocked.
        // A user might be able to get around this by manually modifying the
        // database file itself, but at that point, it's not worth trying to handle.
        // The only other alternative would be to reload the entire filter list every
        // time a SMS is received, which does not scale well to a large number of filters.
        Xlog.i("Registering app package state receiver");

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (!Intent.ACTION_PACKAGE_REMOVED.equals(action) &&
                    !Intent.ACTION_PACKAGE_DATA_CLEARED.equals(action)) {
                    return;
                }

                Uri data = intent.getData();
                if (data == null) {
                    return;
                }

                String packageName = data.getSchemeSpecificPart();
                if (!NEKOSMS_PACKAGE.equals(packageName)) {
                    return;
                }

                if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    Xlog.i("App uninstalled, resetting filters");
                    invalidateCache();
                } else if (Intent.ACTION_PACKAGE_DATA_CLEARED.equals(action)) {
                    Xlog.i("App data cleared, resetting filters");
                    invalidateCache();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_DATA_CLEARED);
        filter.addDataScheme("package");
        mContext.registerReceiver(receiver, filter);
        return receiver;
    }

    private void unregisterContentObserver(ContentObserver observer) {
        mContext.getContentResolver().unregisterContentObserver(observer);
    }

    private void unregisterBroadcastReceiver(BroadcastReceiver receiver) {
        mContext.unregisterReceiver(receiver);
    }
}
