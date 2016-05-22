package com.crossbowffs.nekosms.app;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.utils.XposedUtils;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final String ACTION_OPEN_SECTION = "action_open_section";
    public static final String EXTRA_SECTION = "section";
    public static final String EXTRA_SECTION_FILTER_RULES = "filter_rules";
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCoordinatorLayout = (CoordinatorLayout)findViewById(R.id.main_coordinator);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.main_drawer);
        mNavigationView = (NavigationView)findViewById(R.id.main_navigation);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mFloatingActionButton = (FloatingActionButton)findViewById(R.id.main_fab);

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

        // Restore saved state. Note that the Xposed dialog stuff
        // is only displayed on initial activity creation, to prevent
        // it from being re-displayed when the screen rotates.
        if (savedInstanceState != null) {
            String sectionKey = savedInstanceState.getString(EXTRA_SECTION);
            if (sectionKey != null) {
                setContentSection(sectionKey);
                return;
            }
        } else {
            if (!XposedUtils.isModuleEnabled()) {
                showEnableModuleDialog();
            } else if (XposedUtils.isModuleUpdated()) {
                showModuleUpdatedDialog();
            }
        }
        setContentSection(EXTRA_SECTION_FILTER_RULES);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_SECTION, mContentSection);
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
        case R.id.main_drawer_filter_rules:
            setContentSection(EXTRA_SECTION_FILTER_RULES);
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (ACTION_OPEN_SECTION.equals(intent.getAction())) {
            setContentSection(intent.getStringExtra(EXTRA_SECTION));
        } else {
            if (mContentFragment instanceof MainFragment) {
                ((MainFragment)mContentFragment).onNewIntent(intent);
            }
        }
    }

    private void setContentSection(String key) {
        if (key.equals(mContentSection)) {
            return;
        }

        Fragment fragment;
        int navId;
        switch (key) {
        case EXTRA_SECTION_FILTER_RULES:
            fragment = new FilterRulesFragment();
            navId = R.id.main_drawer_filter_rules;
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
            throw new IllegalArgumentException("Invalid section: " + key);
        }

        dismissSnackbar();
        getFragmentManager()
            .beginTransaction()
            .replace(R.id.main_content, fragment)
            .commit();
        mContentFragment = fragment;
        mContentSection = key;
        mNavigationView.setCheckedItem(navId);
    }

    public void setFabVisible(boolean visible) {
        if (visible) {
            mFloatingActionButton.setTranslationY(0);
            mFloatingActionButton.show();
            mFloatingActionButton.requestLayout();
        } else {
            mFloatingActionButton.hide();
        }
    }

    public void setFabIcon(int iconId) {
        mFloatingActionButton.setImageResource(iconId);
    }

    public void setFabCallback(View.OnClickListener listener) {
        mFloatingActionButton.setOnClickListener(listener);
    }

    public void scrollFabIn() {
        mFloatingActionButton.animate()
            .translationY(0)
            .setInterpolator(new DecelerateInterpolator(2))
            .start();
    }

    public void scrollFabOut() {
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams)mFloatingActionButton.getLayoutParams();
        mFloatingActionButton.animate()
            .translationY(mFloatingActionButton.getHeight() + lp.bottomMargin)
            .setInterpolator(new AccelerateInterpolator(2))
            .start();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
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
            .setNegativeButton(R.string.ignore, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showModuleUpdatedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(R.string.module_outdated_title)
            .setMessage(R.string.module_outdated_message)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.reboot, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startXposedActivity(XposedUtils.XPOSED_SECTION_INSTALL);
                }
            })
            .setNegativeButton(R.string.ignore, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showAboutDialog() {
        Spanned html = Html.fromHtml(getString(R.string.format_about_message,
            TWITTER_URL, GITHUB_URL, ISSUES_URL));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name) + ' ' + VERSION_NAME)
            .setMessage(html)
            .setPositiveButton(R.string.ok, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        TextView textView = (TextView)dialog.findViewById(android.R.id.message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
