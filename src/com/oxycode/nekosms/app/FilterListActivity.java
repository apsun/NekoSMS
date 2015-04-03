package com.oxycode.nekosms.app;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.oxycode.nekosms.R;
import com.oxycode.nekosms.data.SmsFilterData;
import com.oxycode.nekosms.data.SmsFilterField;
import com.oxycode.nekosms.data.SmsFilterLoader;
import com.oxycode.nekosms.data.SmsFilterMode;
import com.oxycode.nekosms.provider.NekoSmsContract;

import java.util.Map;

public class FilterListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static class FilterListItemTag {
        public SmsFilterData mFilterData;
        public TextView mPatternTextView;
        public TextView mInfoTextView;
    }

    private static class FilterAdapter extends ResourceCursorAdapter {
        private int[] mColumns;
        private Map<SmsFilterField, String> mSmsFilterFieldMap;
        private Map<SmsFilterMode, String> mSmsFilterModeMap;

        public FilterAdapter(Context context) {
            super(context, R.layout.listitem_filter_list, null, 0);
            mSmsFilterFieldMap = FilterEnumMaps.getFieldMap(context);
            mSmsFilterModeMap = FilterEnumMaps.getModeMap(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            FilterListItemTag tag = new FilterListItemTag();
            tag.mPatternTextView = (TextView)view.findViewById(R.id.listitem_filter_list_pattern_textview);
            tag.mInfoTextView = (TextView)view.findViewById(R.id.listitem_filter_list_info_textview);
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

            StringBuilder infoBuilder = new StringBuilder();
            infoBuilder.append(mSmsFilterFieldMap.get(field));
            infoBuilder.append(" | ");
            infoBuilder.append(mSmsFilterModeMap.get(mode));
            if (!caseSensitive) {
                infoBuilder.append(" | ");
                infoBuilder.append(context.getString(R.string.filter_ignore_case));
            }

            tag.mPatternTextView.setText(pattern);
            tag.mInfoTextView.setText(infoBuilder.toString());
        }
    }

    private FilterAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_list);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setActionBar(toolbar);

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);

        FilterAdapter adapter = new FilterAdapter(this);
        setListAdapter(adapter);
        mAdapter = adapter;

        registerForContextMenu(getListView());
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
            Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
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

    private void startFilterEditorActivity(long filterId) {
        Intent intent = new Intent(this, FilterEditorActivity.class);
        if (filterId >= 0) {
            Uri filtersUri = NekoSmsContract.Filters.CONTENT_URI;
            Uri filterUri = ContentUris.withAppendedId(filtersUri, filterId);
            intent.setData(filterUri);
        }
        startActivity(intent);
    }

    private void deleteFilter(long filterId) {
        ContentResolver contentResolver = getContentResolver();
        Uri filtersUri = NekoSmsContract.Filters.CONTENT_URI;
        Uri filterUri = ContentUris.withAppendedId(filtersUri, filterId);
        int deletedRows = contentResolver.delete(filterUri, null, null);
        // TODO: Check return value
    }
}
