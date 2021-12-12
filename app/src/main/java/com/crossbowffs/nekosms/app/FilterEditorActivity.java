package com.crossbowffs.nekosms.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FilterEditorActivity extends AppCompatActivity {
    public static final String EXTRA_ACTION = "action";
    private Toolbar mToolbar;
    private FrameLayout mFrameLayout;
    private Uri mFilterUri;
    private SmsFilterData mFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_editor);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mFrameLayout = (FrameLayout)findViewById(R.id.editor_content);

        // Process intent for modifying existing filter if it exists
        mFilterUri = getIntent().getData();
        if (mFilterUri != null) {
            mFilter = FilterRuleLoader.get().query(this, mFilterUri);
        } else {
            mFilter = new SmsFilterData();
            SmsFilterAction action = getAction();
            mFilter.setAction(action);
        }

        // Initialize empty patterns with some reasonable default values
        if (!mFilter.getSenderPattern().hasData()) {
            mFilter.getSenderPattern()
                .setPattern("")
                .setMode(SmsFilterMode.CONTAINS)
                .setCaseSensitive(false);
        }

        if (!mFilter.getBodyPattern().hasData()) {
            mFilter.getBodyPattern()
                .setPattern("")
                .setMode(SmsFilterMode.CONTAINS)
                .setCaseSensitive(false);
        }

        // Set up toolbar 现在使用radioButton 来区分黑白名单
        mToolbar.setTitle(getString(R.string.filter_editor));
        mToolbar.setNavigationIcon(R.drawable.ic_done_white_24dp);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FilterEditorFragment fragment = new FilterEditorFragment();
        getFragmentManager().beginTransaction().replace(R.id.editor_content, fragment).addToBackStack(null).commit();
    }

    private SmsFilterAction getAction() {
        Intent intent = getIntent();
        String actionStr = intent.getStringExtra(EXTRA_ACTION);
        if (actionStr == null) {
            return SmsFilterAction.BLOCK;
        }
        return SmsFilterAction.parse(actionStr);
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

    public SmsFilterPatternData getPatternData(SmsFilterField field) {
        return mFilter.getPatternForField(field);
    }

    public SmsFilterData getSmsFilterData() {
        return mFilter;
    }

    private String validatePatternString(SmsFilterPatternData patternData, int fieldNameId) {
        if (patternData.getMode() != SmsFilterMode.REGEX) {
            return null;
        }
        String pattern = patternData.getPattern();
        try {
            // We don't need the actual compiled pattern, this
            // is just to make sure the syntax is valid
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            String description = e.getDescription();
            if (description == null) {
                description = getString(R.string.invalid_pattern_reason_unknown);
            }
            return getString(R.string.format_invalid_pattern_message, getString(fieldNameId), description);
        }
        return null;
    }

    private boolean shouldSaveFilter() {
        return mFilter.getSenderPattern().hasData() || mFilter.getBodyPattern().hasData();
    }

    private boolean validatePattern(SmsFilterPatternData patternData, int fieldNameId) {
        if (!patternData.hasData()) {
            return true;
        }
        String patternError = validatePatternString(patternData, fieldNameId);
        if (patternError == null) {
            return true;
        }
        showInvalidPatternDialog(patternError);
        return false;
    }

    private void saveIfValid() {
        if (!shouldSaveFilter()) {
            discardAndFinish();
            return;
        }
        if (!validatePattern(mFilter.getSenderPattern(), R.string.invalid_pattern_field_sender)) {
            return;
        }
        if (!validatePattern(mFilter.getBodyPattern(), R.string.invalid_pattern_field_body)) {
            return;
        }
        saveAndFinish();
    }

    private void saveAndFinish() {
        Uri filterUri = persistFilterData();
        int messageId = (filterUri != null) ? R.string.filter_saved : R.string.filter_save_failed;
        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.setData(filterUri);
        setResult(RESULT_OK, intent);
        ActivityCompat.finishAfterTransition(this);
    }

    private void discardAndFinish() {
        setResult(RESULT_CANCELED, null);
        ActivityCompat.finishAfterTransition(this);
    }

    private Uri persistFilterData() {
        return FilterRuleLoader.get().update(this, mFilterUri, mFilter, true);
    }

    private void showInvalidPatternDialog(String errorMessage) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.invalid_pattern_title)
            .setMessage(errorMessage)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.ok, null)
            .show();
    }
}
