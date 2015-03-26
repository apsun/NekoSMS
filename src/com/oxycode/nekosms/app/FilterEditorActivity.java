package com.oxycode.nekosms.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
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
import com.oxycode.nekosms.provider.NekoSmsContract;

import java.util.Arrays;
import java.util.List;

public class FilterEditorActivity extends Activity {
    private EditText mPatternEditText;
    private Spinner mFieldSpinner;
    private Spinner mModeSpinner;
    private Uri mFilterUri;
    private List<String> mSmsFilterFieldKeys;
    private List<String> mSmsFilterModeKeys;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_editor);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("Save filter");
        toolbar.setNavigationIcon(R.drawable.ic_done_white_24dp);
        setActionBar(toolbar);

        Intent intent = getIntent();
        Uri filterUri = intent.getData();

        mFilterUri = filterUri;
        mPatternEditText = (EditText)findViewById(R.id.activity_filter_editor_pattern_edittext);
        mFieldSpinner = (Spinner)findViewById(R.id.activity_filter_editor_field_spinner);
        mModeSpinner = (Spinner)findViewById(R.id.activity_filter_editor_mode_spinner);

        Resources resources = getResources();
        mSmsFilterFieldKeys = Arrays.asList(resources.getStringArray(R.array.sms_filter_field_keys));
        mSmsFilterModeKeys = Arrays.asList(resources.getStringArray(R.array.sms_filter_mode_keys));

        if (filterUri != null) {
            String[] projection = {
                NekoSmsContract.Filters.FIELD,
                NekoSmsContract.Filters.MODE,
                NekoSmsContract.Filters.PATTERN
            };
            Cursor cursor = getContentResolver().query(filterUri, projection, null, null, null);
            cursor.moveToFirst();
            String fieldStr = cursor.getString(0);
            String modeStr = cursor.getString(1);
            String pattern = cursor.getString(2);
            cursor.close();

            mFieldSpinner.setSelection(mSmsFilterFieldKeys.indexOf(fieldStr));
            mModeSpinner.setSelection(mSmsFilterModeKeys.indexOf(modeStr));
            mPatternEditText.setText(pattern);
        }
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
            Uri filterUri = writeFilterData();
            Toast.makeText(this, "Filter saved", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.setData(filterUri);
            setResult(RESULT_OK, intent);
            finish();
            return true;
        case R.id.menu_item_discard_changes:
            setResult(RESULT_CANCELED, null);
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private ContentValues createFilterData() {
        ContentValues values = new ContentValues();
        int fieldIndex = mFieldSpinner.getSelectedItemPosition();
        int modeIndex = mModeSpinner.getSelectedItemPosition();
        String pattern = mPatternEditText.getText().toString();
        values.put(NekoSmsContract.Filters.FIELD, mSmsFilterFieldKeys.get(fieldIndex));
        values.put(NekoSmsContract.Filters.MODE, mSmsFilterModeKeys.get(modeIndex));
        values.put(NekoSmsContract.Filters.PATTERN, pattern);
        return values;
    }

    private Uri writeFilterData() {
        Uri filtersUri = NekoSmsContract.Filters.CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        ContentValues values = createFilterData();
        Uri filterUri;
        if (mFilterUri != null) {
            filterUri = mFilterUri;
            int updatedRows = contentResolver.update(filterUri, values, null, null);
            // TODO: Check return value
        } else {
            filterUri = contentResolver.insert(filtersUri, values);
            // TODO: Check return value
        }

        return filterUri;
    }
}
