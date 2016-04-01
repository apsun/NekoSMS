package com.crossbowffs.nekosms.app;

import android.Manifest;
import android.app.LoaderManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.*;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.backup.BackupLoader;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.database.SmsFilterDbLoader;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;

public class FilterListActivity extends PrivilegedActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private class ScrollListener extends RecyclerView.OnScrollListener {
        private static final int SHOW_THRESHOLD = 50;
        private static final int HIDE_THRESHOLD = 100;
        private int mScrollDistance = 0;
        private boolean mControlsVisible = true;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if ((mControlsVisible && dy > 0) || (!mControlsVisible && dy < 0)) {
                mScrollDistance += dy;
            }

            if (mControlsVisible && mScrollDistance > HIDE_THRESHOLD) {
                mControlsVisible = false;
                mScrollDistance = 0;
                hideCreateButton();
            } else if (!mControlsVisible && mScrollDistance < -SHOW_THRESHOLD) {
                mControlsVisible = true;
                mScrollDistance = 0;
                showCreateButton();
            }
        }
    }

    private static final String TAG = FilterListActivity.class.getSimpleName();
    private static final String VERSION_NAME = BuildConfig.VERSION_NAME;
    private static final String XPOSED_FORUM_URL = "http://forum.xda-developers.com/xposed";
    private static final String TWITTER_URL = "https://twitter.com/crossbowffs";
    private static final String GITHUB_URL = "https://github.com/apsun/NekoSMS";
    private static final String ISSUES_URL = GITHUB_URL + "/issues";
    private static final int IMPORT_BACKUP_REQUEST = 0;
    private static final int EXPORT_BACKUP_REQUEST = 1;

    private FilterListAdapter mAdapter;
    private CoordinatorLayout mCoordinatorLayout;
    private FloatingActionButton mCreateButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_list);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FilterListAdapter adapter = new FilterListAdapter(this);
        mAdapter = adapter;

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);

        mCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.activity_filter_list_root);
        mCreateButton = (FloatingActionButton)findViewById(R.id.activity_filter_list_create_button);
        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FilterListActivity.this, FilterEditorActivity.class);
                startActivity(intent);
            }
        });

        ListRecyclerView recyclerView = (ListRecyclerView)findViewById(R.id.activity_filter_list_recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setEmptyView(findViewById(android.R.id.empty));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnScrollListener(new ScrollListener());
        registerForContextMenu(recyclerView);

        if (!XposedUtils.isModuleEnabled()) {
            showEnableModuleDialog();
        } else if (XposedUtils.getAppVersion() != XposedUtils.getModuleVersion()) {
            showModuleOutdatedDialog();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_filter_list, menu);
        menu.setHeaderTitle(R.string.filter_actions);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListRecyclerView.RecyclerContextMenuInfo info = (ListRecyclerView.RecyclerContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.menu_item_edit_filter:
            startFilterEditorActivity(info.mId);
            return true;
        case R.id.menu_item_delete_filter:
            deleteFilter(info.mId);
            return true;
        default:
            return super.onContextItemSelected(item);
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
        case R.id.menu_item_import_export_filters:
            showImportExportDialog();
            return true;
        case R.id.menu_item_view_blocked_messages:
            startBlockedSmsListActivity();
            return true;
        case R.id.menu_item_settings:
            startSettingsActivity();
            return true;
        case R.id.menu_item_about:
            showAboutDialog();
            return true;
        default:
            return super.onOptionsItemSelected(item);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, boolean granted) {
        if (requestCode == IMPORT_BACKUP_REQUEST || requestCode == EXPORT_BACKUP_REQUEST) {
            if (granted) {
                showImportExportOptionsDialog(requestCode);
            } else {
                Toast.makeText(this, R.string.need_storage_permission, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showCreateButton() {
        mCreateButton.animate()
            .translationY(0)
            .setInterpolator(new DecelerateInterpolator(2))
            .start();
    }

    private void hideCreateButton() {
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams)mCreateButton.getLayoutParams();
        mCreateButton.animate()
            .translationY(mCreateButton.getHeight() + lp.bottomMargin)
            .setInterpolator(new AccelerateInterpolator(2))
            .start();
    }

    private void showImportExportDialog() {
        String importString = getString(R.string.import_from_storage);
        String exportString = getString(R.string.export_to_storage);
        CharSequence[] items = {importString, exportString};
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.import_export)
            .setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        runPrivilegedAction(IMPORT_BACKUP_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE);
                    } else if (which == 1) {
                        runPrivilegedAction(EXPORT_BACKUP_REQUEST, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
            });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showImportExportOptionsDialog(final int requestCode) {
        String filtersString = getString(R.string.backup_option_filters);
        String settingsString = getString(R.string.backup_option_settings);
        CharSequence[] items = {filtersString, settingsString};
        final boolean[] checked = {true, true};
        int titleId;
        int positiveTextId;
        if (requestCode == IMPORT_BACKUP_REQUEST) {
            titleId = R.string.backup_options_dialog_title_import;
            positiveTextId = R.string.backup_button_import;
        } else {
            titleId = R.string.backup_options_dialog_title_export;
            positiveTextId = R.string.backup_button_export;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(titleId)
            .setMultiChoiceItems(items, checked, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    checked[which] = isChecked;
                }
            })
            .setPositiveButton(positiveTextId, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    int options = 0;
                    if (checked[0]) options |= BackupLoader.OPTION_INCLUDE_FILTERS;
                    if (checked[1]) options |= BackupLoader.OPTION_INCLUDE_SETTINGS;
                    if (requestCode == IMPORT_BACKUP_REQUEST) {
                        importFromStorage(options);
                    } else if (requestCode == EXPORT_BACKUP_REQUEST) {
                        exportToStorage(options);
                    }
                }
            })
            .setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void importFromStorage(final int options) {
        new DialogAsyncTask<Void, Void, Integer>(this, R.string.progress_importing) {
            @Override
            protected Integer doInBackground(Void... params) {
                return BackupLoader.importFromStorage(FilterListActivity.this, options);
            }

            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                int messageId;
                switch (result) {
                case BackupLoader.IMPORT_SUCCESS:
                    messageId = R.string.import_success;
                    break;
                case BackupLoader.IMPORT_NO_BACKUP:
                    messageId = R.string.import_no_backup;
                    break;
                case BackupLoader.IMPORT_INVALID_BACKUP:
                    messageId = R.string.import_invalid_backup;
                    break;
                case BackupLoader.IMPORT_READ_FAILED:
                    messageId = R.string.import_read_failed;
                    break;
                case BackupLoader.IMPORT_CANNOT_READ_STORAGE:
                    messageId = R.string.import_cannot_read_storage;
                    break;
                default:
                    throw new AssertionError("Unknown backup import result code: " + result);
                }
                Toast.makeText(FilterListActivity.this, messageId, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void exportToStorage(final int options) {
        new DialogAsyncTask<Void, Void, Integer>(this, R.string.progress_exporting) {
            @Override
            protected Integer doInBackground(Void... params) {
                return BackupLoader.exportToStorage(FilterListActivity.this, options);
            }

            @Override
            protected void onPostExecute(Integer result) {
                super.onPostExecute(result);
                int messageId;
                switch (result) {
                case BackupLoader.EXPORT_SUCCESS:
                    messageId = R.string.export_success;
                    break;
                case BackupLoader.EXPORT_WRITE_FAILED:
                    messageId = R.string.export_write_failed;
                    break;
                case BackupLoader.EXPORT_CANNOT_WRITE_STORAGE:
                    messageId = R.string.export_cannot_write_storage;
                    break;
                default:
                    throw new AssertionError("Unknown backup export result code: " + result);
                }
                Toast.makeText(FilterListActivity.this, messageId, Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void deleteFilter(long filterId) {
        final SmsFilterData filterData = SmsFilterDbLoader.loadAndDeleteFilter(this, filterId);
        if (filterData == null) {
            Xlog.e(TAG, "Failed to delete filter: could not load data");
            return;
        }

        Snackbar.make(mCoordinatorLayout, R.string.filter_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SmsFilterDbLoader.writeFilter(FilterListActivity.this, filterData);
                }
            })
            .show();
    }

    public void startFilterEditorActivity(long id) {
        Intent intent = new Intent(this, FilterEditorActivity.class);
        Uri filterUri = ContentUris.withAppendedId(NekoSmsContract.Filters.CONTENT_URI, id);
        intent.setData(filterUri);
        startActivity(intent);
    }

    private void startBlockedSmsListActivity() {
        Intent intent = new Intent(this, BlockedSmsListActivity.class);
        startActivity(intent);
    }

    private void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void startBrowserActivity(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void startXposedActivity(String section) {
        if (!XposedUtils.startXposedActivity(this, section)) {
            Toast.makeText(this, R.string.xposed_not_installed, Toast.LENGTH_SHORT).show();
            startBrowserActivity(XPOSED_FORUM_URL);
        }
    }

    private void showEnableModuleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.enable_xposed_module_title)
            .setMessage(R.string.enable_xposed_module_message)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startXposedActivity(XposedUtils.XPOSED_SECTION_MODULES);
                }
            })
            .setNeutralButton(R.string.report_bug, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startBrowserActivity(ISSUES_URL);
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
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.reboot, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startXposedActivity(XposedUtils.XPOSED_SECTION_INSTALL);
                }
            })
            .setNegativeButton(R.string.ignore, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showAboutDialog() {
        Spanned html = Html.fromHtml(getString(R.string.format_about_message,
            TWITTER_URL, GITHUB_URL, ISSUES_URL));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name) + " " + VERSION_NAME)
            .setMessage(html)
            .setPositiveButton(R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView textView = (TextView)dialog.findViewById(android.R.id.message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
