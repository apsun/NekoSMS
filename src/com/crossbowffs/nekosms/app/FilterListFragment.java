package com.crossbowffs.nekosms.app;

import android.Manifest;
import android.app.LoaderManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.backup.BackupLoader;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.database.SmsFilterDbLoader;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.Xlog;

public class FilterListFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
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
                scrollFabOut();
            } else if (!mControlsVisible && mScrollDistance < -SHOW_THRESHOLD) {
                mControlsVisible = true;
                mScrollDistance = 0;
                scrollFabIn();
            }
        }
    }

    private static final String TAG = FilterListFragment.class.getSimpleName();
    private static final int IMPORT_BACKUP_REQUEST = 0;
    private static final int EXPORT_BACKUP_REQUEST = 1;

    private ListRecyclerView mFilterListView;
    private View mEmptyView;
    private FilterListAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context darkTheme = new ContextThemeWrapper(getActivity(), R.style.AppTheme);
        LayoutInflater localInflater = inflater.cloneInContext(darkTheme);
        View view = localInflater.inflate(R.layout.fragment_filter_list, container, false);
        mFilterListView = (ListRecyclerView)view.findViewById(R.id.activity_filter_list_recyclerview);
        mEmptyView = view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FilterListAdapter adapter = new FilterListAdapter(this);
        mAdapter = adapter;
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);
        mFilterListView.setAdapter(adapter);
        mFilterListView.setEmptyView(mEmptyView);
        mFilterListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mFilterListView.addOnScrollListener(new ScrollListener());
        registerForContextMenu(mFilterListView);
        setFabVisible(true);
        setFabCallback(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), FilterEditorActivity.class);
                startActivity(intent);
            }
        });
        setTitle(R.string.filter_list);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_filter_list, menu);
        menu.setHeaderTitle(R.string.filter_actions);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListRecyclerView.ContextMenuInfo info = (ListRecyclerView.ContextMenuInfo)item.getMenuInfo();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.options_filter_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_import_export_filters:
            showImportExportDialog();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(),
            NekoSmsContract.Filters.CONTENT_URI,
            NekoSmsContract.Filters.ALL, null, null, null);
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
                showToast(R.string.need_storage_permission);
            }
        }
    }

    private void showImportExportDialog() {
        String importString = getString(R.string.import_from_storage);
        String exportString = getString(R.string.export_to_storage);
        CharSequence[] items = {importString, exportString};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
            .setTitle(R.string.import_export)
            .setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        requestPermissionsCompat(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, IMPORT_BACKUP_REQUEST);
                    } else if (which == 1) {
                        requestPermissionsCompat(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXPORT_BACKUP_REQUEST);
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
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
        new DialogAsyncTask<Void, Void, Integer>(getContext(), R.string.progress_importing) {
            @Override
            protected Integer doInBackground(Void... params) {
                return BackupLoader.importFromStorage(getContext(), options);
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
                showToast(messageId);
            }
        }.execute();
    }

    private void exportToStorage(final int options) {
        new DialogAsyncTask<Void, Void, Integer>(getContext(), R.string.progress_exporting) {
            @Override
            protected Integer doInBackground(Void... params) {
                return BackupLoader.exportToStorage(getContext(), options);
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
                showToast(messageId);
            }
        }.execute();
    }

    private void deleteFilter(long filterId) {
        final SmsFilterData filterData = SmsFilterDbLoader.loadAndDeleteFilter(getContext(), filterId);
        if (filterData == null) {
            Xlog.e(TAG, "Failed to delete filter: could not load data");
            return;
        }

        showSnackbar(R.string.filter_deleted, R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SmsFilterDbLoader.writeFilter(getContext(), filterData);
            }
        });
    }

    public void startFilterEditorActivity(long id) {
        Intent intent = new Intent(getContext(), FilterEditorActivity.class);
        Uri filterUri = ContentUris.withAppendedId(NekoSmsContract.Filters.CONTENT_URI, id);
        intent.setData(filterUri);
        startActivity(intent);
    }
}
