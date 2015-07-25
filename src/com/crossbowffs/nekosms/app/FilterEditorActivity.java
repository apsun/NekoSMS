package com.crossbowffs.nekosms.app;

import android.content.DialogInterface;
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
import com.crossbowffs.nekosms.database.SmsFilterDbLoader;
import com.crossbowffs.nekosms.data.SmsFilterMode;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FilterEditorActivity extends AppCompatActivity {
    private EditText mPatternEditText;
    private Spinner mFieldSpinner;
    private Spinner mModeSpinner;
    private CheckBox mIgnoreCaseCheckBox;
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

        Intent intent = getIntent();
        Uri filterUri = intent.getData();

        mFilterUri = filterUri;
        mPatternEditText = (EditText)findViewById(R.id.activity_filter_editor_pattern_edittext);
        mFieldSpinner = (Spinner)findViewById(R.id.activity_filter_editor_field_spinner);
        mModeSpinner = (Spinner)findViewById(R.id.activity_filter_editor_mode_spinner);
        mIgnoreCaseCheckBox = (CheckBox)findViewById(R.id.activity_filter_editor_ignorecase_checkbox);

        EnumAdapter<SmsFilterField> fieldAdapter = new EnumAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, SmsFilterField.class);
        fieldAdapter.setStringMap(FilterEnumMaps.getFieldMap(this));
        mFieldSpinner.setAdapter(fieldAdapter);

        EnumAdapter<SmsFilterMode> modeAdapter = new EnumAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, SmsFilterMode.class);
        modeAdapter.setStringMap(FilterEnumMaps.getModeMap(this));
        mModeSpinner.setAdapter(modeAdapter);

        SmsFilterData filter = null;
        if (filterUri != null) {
            filter = mFilter = SmsFilterDbLoader.loadFilter(this, filterUri);
        }

        if (filter != null) {
            mFieldSpinner.setSelection(fieldAdapter.getPosition(filter.getField()));
            mModeSpinner.setSelection(modeAdapter.getPosition(filter.getMode()));
            mPatternEditText.setText(filter.getPattern());
            mIgnoreCaseCheckBox.setChecked(!filter.isCaseSensitive());
        } else {
            mFieldSpinner.setSelection(fieldAdapter.getPosition(SmsFilterField.BODY));
            mModeSpinner.setSelection(modeAdapter.getPosition(SmsFilterMode.CONTAINS));
            mIgnoreCaseCheckBox.setChecked(true);
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

    private int getInvalidPatternStringId() {
        SmsFilterMode mode = (SmsFilterMode)mModeSpinner.getSelectedItem();
        if (mode != SmsFilterMode.REGEX) {
            return 0;
        }

        String pattern = mPatternEditText.getText().toString();
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            return R.string.invalid_pattern_regex;
        }

        return 0;
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
        boolean caseSensitive = !mIgnoreCaseCheckBox.isChecked();

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

        int invalidPatternStringId = getInvalidPatternStringId();
        if (invalidPatternStringId != 0) {
            showInvalidPatternDialog(invalidPatternStringId);
            return;
        }

        saveAndFinish();
    }

    private void saveAndFinish() {
        Uri filterUri = writeFilterData();
        if (filterUri != null) {
            Toast.makeText(this, R.string.filter_saved, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.filter_already_exists, Toast.LENGTH_SHORT).show();
        }
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
        boolean caseSensitive = !mIgnoreCaseCheckBox.isChecked();

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

    private void showInvalidPatternDialog(int messageId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.invalid_pattern_title)
            .setMessage(messageId)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    discardAndFinish();
                }
            });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
