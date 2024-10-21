package com.crossbowffs.nekosms.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.*;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.shape.MaterialShapeUtils;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FilterEditorActivity extends AppCompatActivity {
    private static class FilterEditorPageAdapter extends FragmentStateAdapter {
        public FilterEditorPageAdapter(FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            FilterEditorFragment fragment = new FilterEditorFragment();
            Bundle args = new Bundle(1);
            SmsFilterField field;
            if (position == 0) {
                field = SmsFilterField.SENDER;
            } else if (position == 1) {
                field = SmsFilterField.BODY;
            } else {
                throw new AssertionError("Invalid adapter position: " + position);
            }
            args.putSerializable(FilterEditorFragment.EXTRA_FIELD, field);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    public static final String EXTRA_ACTION = "action";

    private AppBarLayout mAppBar;
    private MaterialToolbar mToolbar;
    private TabLayout mTabLayout;
    private ViewPager2 mViewPager;
    private Uri mFilterUri;
    private SmsFilterData mFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_editor);
        mAppBar = findViewById(R.id.appbar);
        mToolbar = findViewById(R.id.toolbar);
        mTabLayout = findViewById(R.id.filter_editor_tablayout);
        mViewPager = findViewById(R.id.filter_editor_viewpager);
        setSupportActionBar(mToolbar);

        // HACK: Fix toolbar tinting on API < 21
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            MaterialShapeUtils.setElevation(mAppBar, 4f * getResources().getDisplayMetrics().density);
        }

        // Set up tab pages
        mViewPager.setAdapter(new FilterEditorPageAdapter(this));
        new TabLayoutMediator(mTabLayout, mViewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(R.string.filter_field_sender);
            } else if (position == 1) {
                tab.setText(R.string.filter_field_body);
            } else {
                throw new AssertionError("Invalid adapter position: " + position);
            }
        }).attach();

        // Process intent for modifying existing filter if it exists
        mFilterUri = getIntent().getData();
        if (mFilterUri != null) {
            mFilter = FilterRuleLoader.get().query(this, mFilterUri);
        } else {
            mFilter = new SmsFilterData();
            SmsFilterAction action = getAction();
            mFilter.setAction(action);
        }

        // Select a tab based on which pattern has data
        // Default to the sender tab if neither has data
        if (!mFilter.getSenderPattern().hasData() && mFilter.getBodyPattern().hasData()) {
            mTabLayout.getTabAt(1).select();
        } else {
            mTabLayout.getTabAt(0).select();
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

        // Set up toolbar
        ActionBar actionBar = getSupportActionBar();
        if (mFilter.getAction() == SmsFilterAction.BLOCK) {
            actionBar.setTitle(R.string.save_blacklist_rule);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_done_24dp);
            actionBar.setHomeActionContentDescription(R.string.save_blacklist_rule);
        } else if (mFilter.getAction() == SmsFilterAction.ALLOW) {
            actionBar.setTitle(R.string.save_whitelist_rule);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_done_24dp);
            actionBar.setHomeActionContentDescription(R.string.save_whitelist_rule);
        }
    }

    @Override
    public void onBackPressed() {
        saveIfValid();
        super.onBackPressed();
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

    private SmsFilterAction getAction() {
        Intent intent = getIntent();
        String actionStr = intent.getStringExtra(EXTRA_ACTION);
        if (actionStr == null) {
            return SmsFilterAction.BLOCK;
        }
        return SmsFilterAction.parse(actionStr);
    }

    private boolean shouldSaveFilter() {
        return mFilter.getSenderPattern().hasData() || mFilter.getBodyPattern().hasData();
    }

    private boolean validatePattern(SmsFilterPatternData patternData, int fieldNameId, int tabIndex) {
        if (!patternData.hasData()) {
            return true;
        }
        String patternError = validatePatternString(patternData, fieldNameId);
        if (patternError == null) {
            return true;
        }
        mTabLayout.getTabAt(tabIndex).select();
        showInvalidPatternDialog(patternError);
        return false;
    }

    private void saveIfValid() {
        if (!shouldSaveFilter()) {
            discardAndFinish();
            return;
        }
        if (!validatePattern(mFilter.getSenderPattern(), R.string.invalid_pattern_field_sender, 0)) {
            return;
        }
        if (!validatePattern(mFilter.getBodyPattern(), R.string.invalid_pattern_field_body, 1)) {
            return;
        }
        saveAndFinish();
    }

    private void saveAndFinish() {
        Uri filterUri = persistFilterData();
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
            .setIcon(R.drawable.ic_warning_24dp)
            .setPositiveButton(R.string.ok, null)
            .show();
    }
}
