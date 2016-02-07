package com.crossbowffs.nekosms.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.database.SmsFilterDbLoader;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FilterEditorActivity extends AppCompatActivity {
    private EditText mPatternEditText;
    private Spinner mFieldSpinner;
    private Spinner mModeSpinner;
    private CheckBox mCaseSensitiveCheckbox;
    private Uri mFilterUri;
    private SmsFilterData mFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_editor);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_done_white_24dp);
        toolbar.setTitle(R.string.save_filter);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPatternEditText = (EditText)findViewById(R.id.activity_filter_editor_pattern_edittext);
        mFieldSpinner = (Spinner)findViewById(R.id.activity_filter_editor_field_spinner);
        mModeSpinner = (Spinner)findViewById(R.id.activity_filter_editor_mode_spinner);
        mCaseSensitiveCheckbox = (CheckBox)findViewById(R.id.activity_filter_editor_case_sensitive_checkbox);

        EnumAdapter<SmsFilterField> fieldAdapter = new EnumAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, SmsFilterField.class);
        fieldAdapter.setStringMap(FilterEnumMaps.getFieldMap(this));
        mFieldSpinner.setAdapter(fieldAdapter);

        EnumAdapter<SmsFilterMode> modeAdapter = new EnumAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, SmsFilterMode.class);
        modeAdapter.setStringMap(FilterEnumMaps.getModeMap(this));
        mModeSpinner.setAdapter(modeAdapter);

        Intent intent = getIntent();
        Uri filterUri = intent.getData();
        SmsFilterData filter = null;
        if (filterUri != null) {
            filter = SmsFilterDbLoader.loadFilter(this, filterUri);
        }
        mFilterUri = filterUri;
        mFilter = filter;

        if (filter != null) {
            mFieldSpinner.setSelection(fieldAdapter.getPosition(filter.getField()));
            mModeSpinner.setSelection(modeAdapter.getPosition(filter.getMode()));
            mPatternEditText.setText(filter.getPattern());
            mCaseSensitiveCheckbox.setChecked(filter.isCaseSensitive());
        } else {
            mFieldSpinner.setSelection(fieldAdapter.getPosition(SmsFilterField.BODY));
            mModeSpinner.setSelection(modeAdapter.getPosition(SmsFilterMode.CONTAINS));
            mCaseSensitiveCheckbox.setChecked(false);
        }
    }

    @Override
    public void onBackPressed() {
        saveIfValid();
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
            saveIfValid();
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

    private String validatePattern() {
        SmsFilterMode mode = (SmsFilterMode)mModeSpinner.getSelectedItem();
        if (mode != SmsFilterMode.REGEX) {
            return null;
        }

        String pattern = mPatternEditText.getText().toString();
        try {
            // We don't need the actual compiled pattern, this
            // is just to make sure the syntax is valid
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            String description = e.getDescription();
            if (description == null) {
                description = getString(R.string.invalid_pattern_reason_unknown);
            }
            return getString(R.string.invalid_pattern_message, description);
        }

        return null;
    }

    private boolean shouldSaveFilter() {
        String pattern = mPatternEditText.getText().toString();

        if (TextUtils.isEmpty(pattern)) {
            return false;
        }

        if (mFilter == null) {
            return true;
        }

        SmsFilterField field = (SmsFilterField)mFieldSpinner.getSelectedItem();
        SmsFilterMode mode = (SmsFilterMode)mModeSpinner.getSelectedItem();
        boolean caseSensitive = mCaseSensitiveCheckbox.isChecked();

        return !(mFilter.getPattern().equals(pattern) &&
                 mFilter.getField() == field &&
                 mFilter.getMode() == mode &&
                 mFilter.isCaseSensitive() == caseSensitive);
    }

    private void saveIfValid() {
        if (!shouldSaveFilter()) {
            discardAndFinish();
            return;
        }

        String errorMessage = validatePattern();
        if (errorMessage != null) {
            showInvalidPatternDialog(errorMessage);
            return;
        }

        saveAndFinish();
    }

    private void saveAndFinish() {
        Uri filterUri = writeFilterData();
        int messageId = (filterUri != null) ? R.string.filter_saved : R.string.filter_already_exists;
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.setData(filterUri);
        setResult(RESULT_OK, intent);
        finishTryTransition();
    }

    private void discardAndFinish() {
        setResult(RESULT_CANCELED, null);
        finishTryTransition();
    }

    private SmsFilterData createFilterData() {
        SmsFilterField field = (SmsFilterField)mFieldSpinner.getSelectedItem();
        SmsFilterMode mode = (SmsFilterMode)mModeSpinner.getSelectedItem();
        String pattern = mPatternEditText.getText().toString();
        boolean caseSensitive = mCaseSensitiveCheckbox.isChecked();

        SmsFilterData data = mFilter;
        if (data == null) {
            data = mFilter = new SmsFilterData();
        }
        data.setField(field);
        data.setMode(mode);
        data.setPattern(pattern);
        data.setCaseSensitive(caseSensitive);
        return data;
    }

    private Uri writeFilterData() {
        SmsFilterData filterData = createFilterData();
        return mFilterUri = SmsFilterDbLoader.updateFilter(this, mFilterUri, filterData, true);
    }

    private void showInvalidPatternDialog(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.invalid_pattern_title)
            .setMessage(errorMessage)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
