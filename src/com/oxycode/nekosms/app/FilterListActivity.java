package com.oxycode.nekosms.app;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toolbar;
import com.oxycode.nekosms.R;
import com.oxycode.nekosms.provider.NekoSmsContract;

public class FilterListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private SimpleCursorAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_list);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setActionBar(toolbar);

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
            this, R.layout.listitem_filter_list, null,
            new String[] { NekoSmsContract.Filters.PATTERN, NekoSmsContract.Filters.FIELD },
            new int[] { R.id.listitem_filter_list_pattern_textview, R.id.listitem_filter_list_info_textview }, 0);
        setListAdapter(adapter);


        registerForContextMenu(getListView());

        mAdapter = adapter;
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
        case R.id.menu_item_import_from_sms:
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
            NekoSmsContract.Filters.FIELD
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

    private void startFilterEditorActivity(long filterId) {
        Intent filterEditorIntent = new Intent(this, FilterEditorActivity.class);
        filterEditorIntent.putExtra(FilterEditorActivity.EXTRA_FILTER_ID, filterId);
        startActivity(filterEditorIntent);
    }

    private void deleteFilter(long filterId) {
        ContentResolver contentResolver = getContentResolver();
        Uri filtersUri = NekoSmsContract.Filters.CONTENT_URI;
        Uri filterUri = ContentUris.withAppendedId(filtersUri, filterId);
        int deletedRows = contentResolver.delete(filterUri, null, null);
        // TODO: Check return value
    }
}
