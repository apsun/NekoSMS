package com.oxycode.nekosms.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Toolbar;
import com.oxycode.nekosms.R;
import com.oxycode.nekosms.data.SmsFilterField;
import com.oxycode.nekosms.data.SmsFilterMode;
import com.oxycode.nekosms.provider.NekoSmsContract;

public class FilterEditorActivity extends Activity {
    public static final String EXTRA_FILTER_ID = "filterId";

    private EditText mPatternEditText;
    private Spinner mFieldSpinner;
    private Spinner mModeSpinner;
    private long mFilterId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_editor);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_done_white_24dp);

        Intent intent = getIntent();
        long filterId = intent.getLongExtra(EXTRA_FILTER_ID, -1);

        mFilterId = filterId;
        mPatternEditText = (EditText)findViewById(R.id.activity_filter_editor_pattern_edittext);
        mFieldSpinner = (Spinner)findViewById(R.id.activity_filter_editor_field_spinner);
        mModeSpinner = (Spinner)findViewById(R.id.activity_filter_editor_mode_spinner);

        if (filterId >= 0) {
            Cursor cursor = getFilterData();
            cursor.moveToFirst();
            SmsFilterField field = SmsFilterField.valueOf(cursor.getString(0));
            SmsFilterMode mode = SmsFilterMode.valueOf(cursor.getString(1));
            String pattern = cursor.getString(2);
            cursor.close();
            // TODO: Set field & mode
            mPatternEditText.setText(pattern);
            toolbar.setTitle("Save filter");
        } else {
            toolbar.setTitle("Add new filter");
        }

        setActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_filter_editor, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            writeFilterData();
            Toast.makeText(this, "Filter saved", Toast.LENGTH_SHORT).show();
            finish();
            return true;
        case R.id.menu_item_discard_changes:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private Cursor getFilterData() {
        ContentResolver contentResolver = getContentResolver();
        Uri filtersUri = NekoSmsContract.Filters.CONTENT_URI;
        Uri filterUri = ContentUris.withAppendedId(filtersUri, mFilterId);
        String[] projection = {
            NekoSmsContract.Filters.FIELD,
            NekoSmsContract.Filters.MODE,
            NekoSmsContract.Filters.PATTERN
        };
        return contentResolver.query(filterUri, projection, null, null, null);
    }

    private ContentValues createFilterData() {
        ContentValues values = new ContentValues();
        SmsFilterField field = SmsFilterField.valueOf((String)mFieldSpinner.getSelectedItem());
        SmsFilterMode mode = SmsFilterMode.valueOf((String)mModeSpinner.getSelectedItem());
        String pattern = mPatternEditText.getText().toString();
        values.put(NekoSmsContract.Filters.MODE, mode.name());
        values.put(NekoSmsContract.Filters.FIELD, field.name());
        values.put(NekoSmsContract.Filters.PATTERN, pattern);
        return values;
    }

    private void writeFilterData() {
        ContentResolver contentResolver = getContentResolver();
        ContentValues values = createFilterData();
        Uri filtersUri = NekoSmsContract.Filters.CONTENT_URI;
        if (mFilterId >= 0) {
            Uri filterUri = ContentUris.withAppendedId(filtersUri, mFilterId);
            int updatedRows = contentResolver.update(filterUri, values, null, null);
            // TODO: Check return value
        } else {
            Uri filterUri = contentResolver.insert(filtersUri, values);
            // TODO: Check return value
        }
    }
}
