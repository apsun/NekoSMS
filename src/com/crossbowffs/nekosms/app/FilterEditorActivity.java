package com.crossbowffs.nekosms.app;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.*;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;
import com.crossbowffs.nekosms.widget.EnumAdapter;
import com.crossbowffs.nekosms.widget.FragmentPagerAdapter;
import com.crossbowffs.nekosms.widget.OnItemSelectedListenerAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FilterEditorActivity extends AppCompatActivity {
    private static class TitleSpinnerAdapter extends EnumAdapter<SmsFilterAction> {
        public TitleSpinnerAdapter(Context context) {
            super(context, R.layout.spinner_item_with_subtitle, android.R.layout.simple_spinner_dropdown_item, SmsFilterAction.class);
            setStringMap(getActionMap(context));
        }

        @Override
        protected void bindMainView(View view, SmsFilterAction item) {
            TextView titleView = (TextView)view.findViewById(R.id.spinner_item_title);
            titleView.setText(getContext().getText(R.string.filter_editor));
            super.bindMainView(view.findViewById(R.id.spinner_item_subtitle), item);
        }
    }

    private class FilterEditorPageAdapter extends FragmentPagerAdapter {
        public FilterEditorPageAdapter() {
            super(getFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            FilterEditorFragment fragment = new FilterEditorFragment();
            Bundle args = new Bundle(1);
            int mode;
            if (position == 0) {
                mode = FilterEditorFragment.EXTRA_MODE_SENDER;
            } else if (position == 1) {
                mode = FilterEditorFragment.EXTRA_MODE_BODY;
            } else {
                throw new AssertionError("Invalid adapter position: " + position);
            }
            args.putInt(FilterEditorFragment.EXTRA_MODE, mode);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return getString(R.string.filter_field_sender);
            } else if (position == 1) {
                return getString(R.string.filter_field_body);
            } else {
                throw new AssertionError("Invalid adapter position: " + position);
            }
        }
    }

    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private Spinner mActionSpinner;
    private TitleSpinnerAdapter mActionAdapter;
    private Uri mFilterUri;
    private SmsFilterData mFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_editor);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mTabLayout = (TabLayout)findViewById(R.id.filter_editor_tablayout);
        mViewPager = (ViewPager)findViewById(R.id.filter_editor_viewpager);
        mActionSpinner = (Spinner)findViewById(R.id.filter_editor_action_spinner);

        // Set up toolbar with spinner
        mToolbar.setNavigationIcon(R.drawable.ic_done_white_24dp);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mActionAdapter = new TitleSpinnerAdapter(this);
        mActionSpinner.setAdapter(mActionAdapter);

        // Set up tab pages
        mViewPager.setAdapter(new FilterEditorPageAdapter());
        mTabLayout.setupWithViewPager(mViewPager);
        mTabLayout.getTabAt(0).select();

        // Process intent for modifying existing filter if it exists
        // Otherwise, default to some reasonable values.
        mFilterUri = getIntent().getData();
        if (mFilterUri != null) {
            mFilter = FilterRuleLoader.get().query(this, mFilterUri);
        } else {
            mFilter = new SmsFilterData().setAction(SmsFilterAction.BLOCK);
        }

        // Ensure sender and body pattern fields are not null
        if (mFilter.getSenderPattern() == null) {
            mFilter.setSenderPattern(new SmsFilterPatternData()
                .setField(SmsFilterField.SENDER)
                .setPattern("")
                .setMode(SmsFilterMode.CONTAINS)
                .setCaseSensitive(false));
        }

        if (mFilter.getBodyPattern() == null) {
            mFilter.setBodyPattern(new SmsFilterPatternData()
                .setField(SmsFilterField.BODY)
                .setPattern("")
                .setMode(SmsFilterMode.CONTAINS)
                .setCaseSensitive(false));
        }

        mActionSpinner.setSelection(mActionAdapter.getPosition(mFilter.getAction()));
        mActionSpinner.setOnItemSelectedListener(new OnItemSelectedListenerAdapter() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mFilter.setAction(mActionAdapter.getItem(position));
            }
        });
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
        switch (field) {
        case SENDER:
            return mFilter.getSenderPattern();
        case BODY:
            return mFilter.getBodyPattern();
        default:
            throw new AssertionError("Invalid filter field: " + field);
        }
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
        if (TextUtils.isEmpty(mFilter.getSenderPattern().getPattern()) &&
            TextUtils.isEmpty(mFilter.getBodyPattern().getPattern())) {
            return false;
        }
        return true;
    }

    private void saveIfValid() {
        if (!shouldSaveFilter()) {
            discardAndFinish();
            return;
        }

        if (!TextUtils.isEmpty(mFilter.getSenderPattern().getPattern())) {
            String senderPatternError = validatePatternString(mFilter.getSenderPattern(), R.string.invalid_pattern_field_sender);
            if (senderPatternError != null) {
                mTabLayout.getTabAt(0).select();
                showInvalidPatternDialog(senderPatternError);
                return;
            }
        }

        if (!TextUtils.isEmpty(mFilter.getBodyPattern().getPattern())) {
            String bodyPatternError = validatePatternString(mFilter.getBodyPattern(), R.string.invalid_pattern_field_body);
            if (bodyPatternError != null) {
                mTabLayout.getTabAt(1).select();
                showInvalidPatternDialog(bodyPatternError);
                return;
            }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.format_invalid_pattern_title)
            .setMessage(errorMessage)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static Map<SmsFilterAction, String> getActionMap(Context context) {
        Resources resources = context.getResources();
        Map<SmsFilterAction, String> fieldMap = new HashMap<>(2);
        fieldMap.put(SmsFilterAction.ALLOW, resources.getString(R.string.filter_action_allow));
        fieldMap.put(SmsFilterAction.BLOCK, resources.getString(R.string.filter_action_block));
        return fieldMap;
    }
}
