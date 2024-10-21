package com.crossbowffs.nekosms.filters;

import android.content.*;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.widget.CursorWrapper;

import java.util.ArrayList;
import java.util.List;

public class SmsFilterLoader {
    private static final String NEKOSMS_PACKAGE = BuildConfig.APPLICATION_ID;

    private final Context mContext;
    private final ContentObserver mContentObserver;
    private final BroadcastReceiver mBroadcastReceiver;
    private List<SmsFilter> mCachedFilters;

    public SmsFilterLoader(Context context) {
        mContext = context;
        mContentObserver = registerContentObserver();
        mBroadcastReceiver = registerBroadcastReceiver();
    }

    public void close() {
        unregisterContentObserver(mContentObserver);
        unregisterBroadcastReceiver(mBroadcastReceiver);
        invalidateCache();
    }

    public boolean shouldBlockMessage(String sender, String body) {
        List<SmsFilter> filters = getFilters();
        if (filters == null) {
            Xlog.i("Allowing message (filters failed to load)");
            return false;
        }

        // Filters are already sorted whitelist first,
        // so we can just return on the first match.
        for (SmsFilter filter : filters) {
            if (filter.match(sender, body)) {
                switch (filter.getAction()) {
                case ALLOW:
                    Xlog.i("Allowing message (matched whitelist)");
                    return false;
                case BLOCK:
                    Xlog.i("Blocking message (matched blacklist)");
                    return true;
                }
            }
        }

        Xlog.i("Allowing message (did not match any rules)");
        return false;
    }

    private List<SmsFilter> getFilters() {
        List<SmsFilter> filters = mCachedFilters;
        if (filters == null) {
            Xlog.i("Cached SMS filters dirty, loading from database");
            filters = mCachedFilters = loadFilters();
        }
        return filters;
    }

    private void invalidateCache() {
        mCachedFilters = null;
    }

    private List<SmsFilter> loadFilters() {
        try (CursorWrapper<SmsFilterData> filterCursor = FilterRuleLoader.get().queryAll(mContext)) {
            if (filterCursor == null) {
                // This might occur if the app has been uninstalled (removing the DB),
                // but the user has not rebooted their device yet. We should not filter
                // any messages in this state.
                Xlog.e("Failed to load SMS filters (queryAll returned null)");
                return null;
            }

            int count = filterCursor.getCount();
            Xlog.i("filterCursor.getCount() = %d", count);

            // It's better to just over-reserve since we expect most
            // rules to go into the blacklist, but all rules will
            // be merged into the whitelist list in the end (with
            // whitelist rules coming first).
            ArrayList<SmsFilter> whitelist = new ArrayList<>(count);
            ArrayList<SmsFilter> blacklist = new ArrayList<>(count);

            SmsFilterData data = new SmsFilterData();
            while (filterCursor.moveToNext()) {
                SmsFilter filter;
                try {
                    data = filterCursor.get(data);
                    filter = new SmsFilter(data);
                } catch (Exception e) {
                    Xlog.e("Failed to load SMS filter", e);
                    continue;
                }

                if (data.getAction() == SmsFilterAction.BLOCK) {
                    blacklist.add(filter);
                } else if (data.getAction() == SmsFilterAction.ALLOW) {
                    whitelist.add(filter);
                }
            }

            Xlog.i("Loaded %d blacklist filters", blacklist.size());
            Xlog.i("Loaded %d whitelist filters", whitelist.size());
            whitelist.addAll(blacklist);
            whitelist.trimToSize();
            return whitelist;
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
                Uri data = intent.getData();
                if (data == null) {
                    return;
                }

                String packageName = data.getSchemeSpecificPart();
                if (!NEKOSMS_PACKAGE.equals(packageName)) {
                    return;
                }

                String action = intent.getAction();
                if (action == null) {
                    return;
                }

                switch (action) {
                case Intent.ACTION_PACKAGE_REMOVED:
                    Xlog.i("App uninstalled, resetting filters");
                    invalidateCache();
                    break;
                case Intent.ACTION_PACKAGE_DATA_CLEARED:
                    Xlog.i("App data cleared, resetting filters");
                    invalidateCache();
                    break;
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
