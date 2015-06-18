package com.crossbowffs.nekosms.app;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.*;
import android.widget.*;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterLoader;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.XposedUtils;

import java.util.Map;

public class FilterListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static class FilterListItemTag {
        public SmsFilterData mFilterData;
        public TextView mPatternTextView;
        public TextView mFieldTextView;
        public TextView mModeTextView;
        public TextView mCaseSensitiveTextView;
    }

    private static class FilterAdapter extends ResourceCursorAdapter {
        private int[] mColumns;
        private Map<SmsFilterField, String> mSmsFilterFieldMap;
        private Map<SmsFilterMode, String> mSmsFilterModeMap;
        private String mPatternFormatString;
        private String mFieldFormatString;
        private String mModeFormatString;
        private String mCaseSensitiveFormatString;

        public FilterAdapter(Context context) {
            super(context, R.layout.listitem_filter_list, null, 0);
            mSmsFilterFieldMap = FilterEnumMaps.getFieldMap(context);
            mSmsFilterModeMap = FilterEnumMaps.getModeMap(context);

            Resources resources = context.getResources();
            mPatternFormatString = resources.getString(R.string.format_filter_pattern);
            mFieldFormatString = resources.getString(R.string.format_filter_field);
            mModeFormatString = resources.getString(R.string.format_filter_mode);
            mCaseSensitiveFormatString = resources.getString(R.string.format_filter_case_sensitive);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            FilterListItemTag tag = new FilterListItemTag();
            tag.mPatternTextView = (TextView)view.findViewById(R.id.listitem_filter_list_pattern_textview);
            tag.mFieldTextView = (TextView)view.findViewById(R.id.listitem_filter_list_field_textview);
            tag.mModeTextView = (TextView)view.findViewById(R.id.listitem_filter_list_mode_textview);
            tag.mCaseSensitiveTextView = (TextView)view.findViewById(R.id.listitem_filter_list_case_sensitive_textview);
            view.setTag(tag);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            if (mColumns == null) {
                mColumns = SmsFilterLoader.getColumns(cursor);
            }

            FilterListItemTag tag = (FilterListItemTag)view.getTag();
            SmsFilterData filterData = SmsFilterLoader.getFilterData(cursor, mColumns, tag.mFilterData);
            tag.mFilterData = filterData;

            SmsFilterField field = filterData.getField();
            SmsFilterMode mode = filterData.getMode();
            String pattern = filterData.getPattern();
            boolean caseSensitive = filterData.isCaseSensitive();

            tag.mPatternTextView.setText(String.format(mPatternFormatString, pattern));
            tag.mFieldTextView.setText(String.format(mFieldFormatString, mSmsFilterFieldMap.get(field)));
            tag.mModeTextView.setText(String.format(mModeFormatString, mSmsFilterModeMap.get(mode)));
            tag.mCaseSensitiveTextView.setText(String.format(mCaseSensitiveFormatString, caseSensitive));
        }
    }

    private static final String TWITTER_URL = "https://twitter.com/crossbowffs";
    private static final String BITBUCKET_URL = "https://bitbucket.org/crossbowffs/nekosms";
    private static final String REPORT_BUG_URL = BITBUCKET_URL + "/issues/new";

    private View mContentView;
    private FilterAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_list);

        mContentView = findViewById(android.R.id.content);

        FilterAdapter adapter = new FilterAdapter(this);
        mAdapter = adapter;

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);

        ListView listView = (ListView)findViewById(android.R.id.list);
        listView.setEmptyView(findViewById(android.R.id.empty));
        listView.setAdapter(adapter);
        registerForContextMenu(listView);

        if (!XposedUtils.isModuleEnabled()) {
            showInitFailedDialog();
        } else if (XposedUtils.getAppVersion() != XposedUtils.getModuleVersion()) {
            showModuleOutdatedDialog();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_filter_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_create_filter:
            startFilterEditorActivity(-1);
            return true;
        case R.id.menu_item_view_blocked_messages:
            startBlockedSmsListActivity();
            return true;
        case R.id.menu_item_about:
            showAboutDialog();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_filter_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        long rowId = info.id;
        switch (item.getItemId()) {
        case R.id.contextmenu_filter_list_edit:
            startFilterEditorActivity(rowId);
            return true;
        case R.id.contextmenu_filter_list_delete:
            deleteFilter(rowId);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] from = {
            NekoSmsContract.Filters._ID,
            NekoSmsContract.Filters.PATTERN,
            NekoSmsContract.Filters.MODE,
            NekoSmsContract.Filters.FIELD,
            NekoSmsContract.Filters.CASE_SENSITIVE
        };
        Uri uri = NekoSmsContract.Filters.CONTENT_URI;
        return new CursorLoader(this, uri, from, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    private void finishTryTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    private void startBlockedSmsListActivity() {
        Intent intent = new Intent(this, BlockedSmsListActivity.class);
        startActivity(intent);
    }

    private void startFilterEditorActivity(long filterId) {
        Intent intent = new Intent(this, FilterEditorActivity.class);
        if (filterId >= 0) {
            Uri filterUri = ContentUris.withAppendedId(NekoSmsContract.Filters.CONTENT_URI, filterId);
            intent.setData(filterUri);
        }
        startActivity(intent);
    }

    private void deleteFilter(long filterId) {
        final SmsFilterData filterData = SmsFilterLoader.loadAndDeleteFilter(this, filterId);
        Snackbar.make(mContentView, R.string.filter_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SmsFilterLoader.writeFilter(FilterListActivity.this, filterData);
                }
            })
            .show();
    }

    private String getPackageVersion() {
        try {
            PackageManager packageManager = getPackageManager();
            String packageName = getPackageName();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void showInitFailedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.module_not_enabled_title)
            .setMessage(R.string.module_not_enabled_message)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setCancelable(false)
            .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finishTryTransition();
                }
            })
            .setNeutralButton(R.string.report_bug, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finishTryTransition();
                    Uri url = Uri.parse(REPORT_BUG_URL);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, url);
                    startActivity(browserIntent);
                }
            })
            .setNegativeButton(R.string.ignore, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showModuleOutdatedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.module_outdated_title)
            .setMessage(R.string.module_outdated_message)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setCancelable(false)
            .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finishTryTransition();
                }
            })
            .setNegativeButton(R.string.ignore, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showAboutDialog() {
        Spanned html = Html.fromHtml(getString(R.string.format_about_message,
            TWITTER_URL, BITBUCKET_URL, REPORT_BUG_URL));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name) + " " + getPackageVersion())
            .setMessage(html)
            .setPositiveButton(R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView textView = (TextView)dialog.findViewById(android.R.id.message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
