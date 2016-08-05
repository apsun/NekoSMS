package com.crossbowffs.nekosms.app;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;
import com.crossbowffs.nekosms.widget.FabHideOnScrollBehavior;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String ACTION_OPEN_SECTION = "action_open_section";
    public static final String EXTRA_SECTION = "section";
    public static final String EXTRA_SECTION_BLACKLIST_RULES = "blacklist_rules";
    public static final String EXTRA_SECTION_WHITELIST_RULES = "whitelist_rules";
    public static final String EXTRA_SECTION_BLOCKED_MESSAGES = "blocked_messages";
    public static final String EXTRA_SECTION_SETTINGS = "settings";

    private static final String VERSION_NAME = BuildConfig.VERSION_NAME;
    private static final String XPOSED_FORUM_URL = "http://forum.xda-developers.com/xposed";
    private static final String TWITTER_URL = "https://twitter.com/crossbowffs";
    private static final String GITHUB_URL = "https://github.com/apsun/NekoSMS";
    private static final String ISSUES_URL = GITHUB_URL + "/issues";

    private CoordinatorLayout mCoordinatorLayout;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private Toolbar mToolbar;
    private FloatingActionButton mFloatingActionButton;
    private ActionBarDrawerToggle mDrawerToggle;
    private Set<Snackbar> mSnackbars;
    private Fragment mContentFragment;
    private String mContentSection;
    private SharedPreferences mInternalPrefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.main_coordinator);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.main_drawer);
        mNavigationView = (NavigationView)findViewById(R.id.main_navigation);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mFloatingActionButton = (FloatingActionButton)findViewById(R.id.main_fab);

        // Load preferences
        mInternalPrefs = getSharedPreferences(PreferenceConsts.FILE_INTERNAL, MODE_PRIVATE);

        // Setup toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Setup navigation drawer
        mNavigationView.setNavigationItemSelectedListener(this);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.open_drawer, R.string.close_drawer);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        // This is used to cache displayed snackbars, so we can
        // dismiss them when switching between fragments.
        mSnackbars = Collections.newSetFromMap(new WeakHashMap<Snackbar, Boolean>());

        // Process intent. Open a specific section if necessary.
        Intent intent = getIntent();
        if (intent != null && ACTION_OPEN_SECTION.equals(intent.getAction())) {
            setContentSection(intent.getStringExtra(EXTRA_SECTION));
            return;
        }

        // Xposed dialog stuff is only displayed on initial activity creation,
        // to prevent it from being re-displayed when the screen rotates.
        if (savedInstanceState == null) {
            if (!XposedUtils.isModuleEnabled()) {
                showEnableModuleDialog();
            } else if (XposedUtils.isModuleUpdated()) {
                showModuleUpdatedDialog();
            }
        }

        // Set the section that was selected previously
        String section = mInternalPrefs.getString(PreferenceConsts.KEY_SELECTED_SECTION, EXTRA_SECTION_BLACKLIST_RULES);
        setContentSection(section);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Workaround for a weird issue where the drawer state
        // hasn't been restored in onPostCreate, leaving the arrow
        // state out-of-sync if the drawer is open.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        mDrawerLayout.closeDrawer(mNavigationView);
        switch (item.getItemId()) {
        case R.id.main_drawer_blacklist_rules:
            setContentSection(EXTRA_SECTION_BLACKLIST_RULES);
            return true;
        case R.id.main_drawer_whitelist_rules:
            setContentSection(EXTRA_SECTION_WHITELIST_RULES);
            return true;
        case R.id.main_drawer_blocked_messages:
            setContentSection(EXTRA_SECTION_BLOCKED_MESSAGES);
            return true;
        case R.id.main_drawer_settings:
            setContentSection(EXTRA_SECTION_SETTINGS);
            return true;
        case R.id.main_drawer_about:
            showAboutDialog();
            return true;
        default:
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (ACTION_OPEN_SECTION.equals(intent.getAction())) {
            if (setContentSection(intent.getStringExtra(EXTRA_SECTION))) {
                return;
            }
        }
        if (mContentFragment instanceof MainFragment) {
            ((MainFragment)mContentFragment).onNewIntent(intent);
        }
    }

    private boolean setContentSection(String key) {
        if (key.equals(mContentSection)) {
            return false;
        }

        Fragment fragment;
        int navId;
        switch (key) {
        case EXTRA_SECTION_BLACKLIST_RULES:
            fragment = new FilterRulesFragment();
            Bundle argsB = new Bundle(1);
            argsB.putSerializable(FilterRulesFragment.EXTRA_ACTION, SmsFilterAction.BLOCK);
            fragment.setArguments(argsB);
            navId = R.id.main_drawer_blacklist_rules;
            break;
        case EXTRA_SECTION_WHITELIST_RULES:
            fragment = new FilterRulesFragment();
            Bundle argsW = new Bundle(1);
            argsW.putSerializable(FilterRulesFragment.EXTRA_ACTION, SmsFilterAction.ALLOW);
            fragment.setArguments(argsW);
            navId = R.id.main_drawer_whitelist_rules;
            break;
        case EXTRA_SECTION_BLOCKED_MESSAGES:
            fragment = new BlockedMessagesFragment();
            navId = R.id.main_drawer_blocked_messages;
            break;
        case EXTRA_SECTION_SETTINGS:
            fragment = new SettingsFragment();
            navId = R.id.main_drawer_settings;
            break;
        default:
            Xlog.e(TAG, "Unknown context section: %s", key);
            return setContentSection(EXTRA_SECTION_BLACKLIST_RULES);
        }

        dismissSnackbar();
        getFragmentManager()
            .beginTransaction()
            .replace(R.id.main_content, fragment)
            .commit();
        mContentFragment = fragment;
        mContentSection = key;
        mNavigationView.setCheckedItem(navId);
        mInternalPrefs.edit().putString(PreferenceConsts.KEY_SELECTED_SECTION, key).apply();
        return true;
    }

    public void setFabVisible(boolean visible) {
        if (visible) {
            mFloatingActionButton.show();
        } else {
            mFloatingActionButton.hide();
        }

        // Also make sure the button stops responding to scrolling
        // if hidden, to prevent it from re-showing itself
        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)mFloatingActionButton.getLayoutParams();
        FabHideOnScrollBehavior behavior = (FabHideOnScrollBehavior)params.getBehavior();
        behavior.setAutoHideEnabled(visible);
    }

    public void setFabIcon(int iconId) {
        mFloatingActionButton.setImageResource(iconId);
    }

    public void setFabCallback(View.OnClickListener listener) {
        mFloatingActionButton.setOnClickListener(listener);
    }

    public Snackbar makeSnackbar(int textId) {
        Snackbar snackbar = Snackbar.make(mCoordinatorLayout, textId, Snackbar.LENGTH_LONG);
        mSnackbars.add(snackbar);
        return snackbar;
    }

    public void dismissSnackbar() {
        // Items will be automatically removed from the cache
        // once the references are GC'd
        for (Snackbar snackbar : mSnackbars) {
            snackbar.dismiss();
        }
    }

    private void startBrowserActivity(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    private void startXposedActivity(String section) {
        if (!XposedUtils.startXposedActivity(this, section)) {
            Toast.makeText(this, R.string.xposed_not_installed, Toast.LENGTH_SHORT).show();
            startBrowserActivity(XPOSED_FORUM_URL);
        }
    }

    private void showEnableModuleDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.enable_xposed_module_title)
            .setMessage(R.string.enable_xposed_module_message)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startXposedActivity(XposedUtils.XPOSED_SECTION_MODULES);
                }
            })
            .setNeutralButton(R.string.report_bug, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startBrowserActivity(ISSUES_URL);
                }
            })
            .setNegativeButton(R.string.ignore, null)
            .show();
    }

    private void showModuleUpdatedDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.module_outdated_title)
            .setMessage(R.string.module_outdated_message)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.reboot, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startXposedActivity(XposedUtils.XPOSED_SECTION_INSTALL);
                }
            })
            .setNegativeButton(R.string.ignore, null)
            .show();
    }

    private void showAboutDialog() {
        Spanned html = Html.fromHtml(getString(R.string.format_about_message,
            TWITTER_URL, GITHUB_URL, ISSUES_URL));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name) + ' ' + VERSION_NAME)
            .setMessage(html)
            .setPositiveButton(R.string.ok, null)
            .show();

        TextView textView = (TextView)dialog.findViewById(android.R.id.message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
