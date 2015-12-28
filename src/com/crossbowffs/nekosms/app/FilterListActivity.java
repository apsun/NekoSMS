package com.crossbowffs.nekosms.app;

import android.Manifest;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.backup.BackupLoader;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
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
    private static final String TWITTER_URL = "https://twitter.com/crossbowffs";
    private static final String GITHUB_URL = "https://github.com/apsun/NekoSMS";
    private static final String ISSUES_URL = GITHUB_URL + "/issues";
    private static final int IMPORT_FILTERS_REQUEST = 0;
    private static final int EXPORT_FILTERS_REQUEST = 1;

    private FilterListAdapter mAdapter;
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

        FloatingActionButton createButton = (FloatingActionButton)findViewById(R.id.activity_filter_list_create_button);
        mCreateButton = createButton;
        createButton.setOnClickListener(new View.OnClickListener() {
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
        if (requestCode == IMPORT_FILTERS_REQUEST) {
            if (granted) {
                importFromStorage();
            } else {
                Toast.makeText(this, R.string.need_storage_read_permission, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == EXPORT_FILTERS_REQUEST) {
            if (granted) {
                exportToStorage();
            } else {
                Toast.makeText(this, R.string.need_storage_write_permission, Toast.LENGTH_SHORT).show();
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
                        runPrivilegedAction(IMPORT_FILTERS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE);
                    } else if (which == 1) {
                        runPrivilegedAction(EXPORT_FILTERS_REQUEST, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
            });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void importFromStorage() {
        int result = BackupLoader.importFromStorage(this);
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
        default:
            throw new AssertionError("Unknown backup import result code: " + result);
        }
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }

    private void exportToStorage() {
        int result = BackupLoader.exportToStorage(this);
        int messageId;
        switch (result) {
        case BackupLoader.EXPORT_SUCCESS:
            messageId = R.string.export_success;
            break;
        case BackupLoader.EXPORT_WRITE_FAILED:
            messageId = R.string.export_write_failed;
            break;
        default:
            throw new AssertionError("Unknown backup export result code: " + result);
        }
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
    }

    private void startBlockedSmsListActivity() {
        Intent intent = new Intent(this, BlockedSmsListActivity.class);
        startActivity(intent);
    }

    private void startSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void startReportBugActivity() {
        Uri url = Uri.parse(ISSUES_URL);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, url);
        startActivity(browserIntent);
    }

    private void startXposedActivity(String section) {
        if (!XposedUtils.startXposedActivity(this, section)) {
            Toast.makeText(this, R.string.xposed_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showInitFailedDialog() {
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
                    startReportBugActivity();
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
            .setTitle(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME)
            .setMessage(html)
            .setPositiveButton(R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView textView = (TextView)dialog.findViewById(android.R.id.message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
