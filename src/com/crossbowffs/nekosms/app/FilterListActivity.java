package com.crossbowffs.nekosms.app;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.*;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.TextView;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterLoader;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.XposedUtils;

public class FilterListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
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

    private static final String TWITTER_URL = "https://twitter.com/crossbowffs";
    private static final String BITBUCKET_URL = "https://bitbucket.org/crossbowffs/nekosms";
    private static final String REPORT_BUG_URL = BITBUCKET_URL + "/issues/new";

    private View mCoordinatorLayout;
    private FilterListAdapter mAdapter;
    private FloatingActionButton mCreateButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_list);

        mCoordinatorLayout = findViewById(R.id.activity_filter_list_root);

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
                startFilterEditorActivity(-1);
            }
        });

        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.activity_filter_list_recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnScrollListener(new ScrollListener());
        registerForContextMenu(recyclerView);

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
            // TODO
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

    public void showCreateButton() {
        mCreateButton.animate()
            .translationY(0)
            .setInterpolator(new DecelerateInterpolator(2))
            .start();
    }

    public void hideCreateButton() {
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams)mCreateButton.getLayoutParams();
        mCreateButton.animate()
            .translationY(mCreateButton.getHeight() + lp.bottomMargin)
            .setInterpolator(new AccelerateInterpolator(2))
            .start();
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
        Snackbar.make(mCoordinatorLayout, R.string.filter_deleted, Snackbar.LENGTH_LONG)
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
