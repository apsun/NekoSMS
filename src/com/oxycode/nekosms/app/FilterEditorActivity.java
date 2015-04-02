package com.oxycode.nekosms.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.*;
import com.oxycode.nekosms.R;
import com.oxycode.nekosms.data.*;
import com.oxycode.nekosms.provider.NekoSmsContract;

import java.util.Arrays;
import java.util.List;

public class FilterEditorActivity extends Activity {
    private EditText mPatternEditText;
    private Spinner mFieldSpinner;
    private Spinner mModeSpinner;
    private CheckBox mIgnoreCaseCheckBox;
    private Uri mFilterUri;
    private SmsFilterData mFilter;
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
        mIgnoreCaseCheckBox = (CheckBox)findViewById(R.id.activity_filter_editor_ignorecase_checkbox);

        Resources resources = getResources();
        mSmsFilterFieldKeys = Arrays.asList(resources.getStringArray(R.array.sms_filter_field_keys));
        mSmsFilterModeKeys = Arrays.asList(resources.getStringArray(R.array.sms_filter_mode_keys));

        if (filterUri != null) {
            SmsFilterData filter = SmsFilterLoader.loadFilter(this, filterUri);
            mFilter = filter;
            mFieldSpinner.setSelection(mSmsFilterFieldKeys.indexOf(filter.getField().name()));
            mModeSpinner.setSelection(mSmsFilterModeKeys.indexOf(filter.getMode().name()));
            mPatternEditText.setText(filter.getPattern());
            mIgnoreCaseCheckBox.setChecked(filter.isCaseSensitive());
        }
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
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
            saveAndFinish();
            return true;
        case R.id.menu_item_discard_changes:
            discardAndFinish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void finishTryTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    private void saveAndFinish() {
        Uri filterUri = writeFilterData();
        Toast.makeText(this, "Filter saved", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.setData(filterUri);
        setResult(RESULT_OK, intent);
        finishTryTransition();
    }

    private void discardAndFinish() {
        setResult(RESULT_CANCELED, null);
        finishTryTransition();
    }

    private ContentValues createFilterData() {
        int fieldIndex = mFieldSpinner.getSelectedItemPosition();
        int modeIndex = mModeSpinner.getSelectedItemPosition();
        String pattern = mPatternEditText.getText().toString();
        boolean caseSensitive = mIgnoreCaseCheckBox.isChecked();

        SmsFilterData data = mFilter;
        if (data == null) {
            data = mFilter = new SmsFilterData();
        }
        data.setField(SmsFilterField.valueOf(mSmsFilterFieldKeys.get(fieldIndex)));
        data.setMode(SmsFilterMode.valueOf(mSmsFilterModeKeys.get(modeIndex)));
        data.setPattern(pattern);
        data.setCaseSensitive(caseSensitive);
        return data.serialize();
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
