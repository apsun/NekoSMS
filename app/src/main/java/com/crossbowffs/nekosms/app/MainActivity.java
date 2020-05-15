package com.crossbowffs.nekosms.app;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.consts.PreferenceConsts;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.IOUtils;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;

import java.util.*;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final String EXTRA_SECTION = "section";
    public static final String EXTRA_SECTION_BLACKLIST_RULES = "blacklist_rules";
    public static final String EXTRA_SECTION_WHITELIST_RULES = "whitelist_rules";
    public static final String EXTRA_SECTION_BLOCKED_MESSAGES = "blocked_messages";
    public static final String EXTRA_SECTION_SETTINGS = "settings";

    private static final String VERSION_NAME = BuildConfig.VERSION_NAME;
    private static final int VERSION_CODE = BuildConfig.VERSION_CODE;
    private static final String GITHUB_URL = "https://github.com/apsun/NekoSMS";
    private static final String WIKI_URL = GITHUB_URL + "/wiki";

    private static final String[] TASK_KILLER_PACKAGES = {
        "me.piebridge.forcestopgb",
        "com.oasisfeng.greenify",
        "me.piebridge.brevent",
        "com.click369.controlbp",
    };

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
        mInternalPrefs.edit().putInt(PreferenceConsts.KEY_APP_VERSION, VERSION_CODE).apply();

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

        // Create the notification channel immediately so user can
        // configure them immediately without needing to receive
        // a notification first
        NotificationHelper.createNotificationChannel(this);

        // Don't do this if the activity is being re-created (e.g.
        // after a screen rotation), since it will cause the fragment
        // to be created twice (see http://stackoverflow.com/a/13306633/)
        if (savedInstanceState == null) {
            // Process intent. If an action was taken, don't do the rest.
            if (handleIntent(getIntent())) {
                return;
            }

            // Show info dialogs as necessary
            if (!XposedUtils.isModuleEnabled()) {
                if (XposedUtils.isXposedInstalled(this)) {
                    showEnableModuleDialog();
                } else {
                    // We should probably show a different dialog if the
                    // user doesn't even have Xposed installed...
                    showEnableModuleDialog();
                }
            } else if (XposedUtils.isModuleUpdated()) {
                showModuleUpdatedDialog();
            } else {
                showTaskKillerDialogIfNecessary();
            }

            // Set the section that was selected previously
            String section = mInternalPrefs.getString(PreferenceConsts.KEY_SELECTED_SECTION, EXTRA_SECTION_BLACKLIST_RULES);
            setContentSection(section);
        }
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
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
        handleIntent(intent);
    }

    private boolean handleIntent(Intent intent) {
        if (intent == null) {
            return false;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                if (IOUtils.isParentUri(DatabaseContract.BlockedMessages.CONTENT_URI, uri)) {
                    Xlog.i("Got ACTION_VIEW intent with blocked message URI");
                    Bundle args = new Bundle(1);
                    args.putParcelable(BlockedMessagesFragment.ARG_MESSAGE_URI, uri);
                    setContentSection(EXTRA_SECTION_BLOCKED_MESSAGES, args);
                } else {
                    // Treat all other ACTION_VIEW intents as backup import requests.
                    // If we turn out to be wrong, at worst we just get an invalid
                    // file error.
                    Xlog.i("Got ACTION_VIEW intent with (maybe) backup file URI");
                    Bundle args = new Bundle(1);
                    args.putParcelable(FilterRulesFragment.ARG_IMPORT_URI, uri);
                    setContentSection(EXTRA_SECTION_BLACKLIST_RULES, args);
                }

                // Kind of a hacky workaround; this ensures that we only execute the
                // action once (in case intent gets re-delivered).
                intent.setData(null);
                return true;
            }
        }

        // If we didn't process the intent already, respond to
        // content section setting intents.
        String section = intent.getStringExtra(EXTRA_SECTION);
        if (section != null) {
            intent.removeExtra(EXTRA_SECTION);
            setContentSection(section);
            return true;
        }

        return false;
    }

    private boolean setContentSection(String key, Bundle args) {
        // If our target section is already selected, just update
        // its arguments and return.
        if (key.equals(mContentSection)) {
            if (args != null) {
                if (mContentFragment instanceof MainFragment) {
                    ((MainFragment)mContentFragment).onNewArguments(args);
                }
            }
            return false;
        }

        Fragment fragment;
        int navId;
        switch (key) {
        case EXTRA_SECTION_BLACKLIST_RULES:
            fragment = new FilterRulesFragment();
            if (args == null) {
                args = new Bundle();
            }
            args.putString(FilterRulesFragment.EXTRA_ACTION, SmsFilterAction.BLOCK.name());
            navId = R.id.main_drawer_blacklist_rules;
            break;
        case EXTRA_SECTION_WHITELIST_RULES:
            fragment = new FilterRulesFragment();
            if (args == null) {
                args = new Bundle();
            }
            args.putString(FilterRulesFragment.EXTRA_ACTION, SmsFilterAction.ALLOW.name());
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
            Xlog.e("Unknown context section: %s", key);
            return setContentSection(EXTRA_SECTION_BLACKLIST_RULES);
        }

        if (args != null) {
            fragment.setArguments(args);
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

    private boolean setContentSection(String key) {
        return setContentSection(key, null);
    }

    public void enableFab(int iconId, View.OnClickListener listener) {
        mFloatingActionButton.setImageResource(iconId);
        mFloatingActionButton.setOnClickListener(listener);
        mFloatingActionButton.show();
    }

    public void disableFab() {
        mFloatingActionButton.setOnClickListener(null);
        mFloatingActionButton.hide();
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

    private void startXposedActivity(XposedUtils.Section section) {
        if (!XposedUtils.startXposedActivity(this, section)) {
            Toast.makeText(this, R.string.xposed_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean shouldShowTaskKillerDialog(List<PackageInfo> taskKillers) {
        if (taskKillers.isEmpty()) {
            return false;
        }

        Set<String> knownTaskKillers = mInternalPrefs.getStringSet(PreferenceConsts.KEY_KNOWN_TASK_KILLERS, null);
        if (knownTaskKillers == null) {
            return true;
        }

        for (PackageInfo pkgInfo : taskKillers) {
            if (!knownTaskKillers.contains(pkgInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    private List<PackageInfo> getInstalledTaskKillers() {
        PackageManager packageManager = getPackageManager();
        ArrayList<PackageInfo> apps = new ArrayList<>();
        for (String pkgName : TASK_KILLER_PACKAGES) {
            PackageInfo pkgInfo;
            try {
                pkgInfo = packageManager.getPackageInfo(pkgName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }
            apps.add(pkgInfo);
        }
        return apps;
    }

    private String getAppDisplayName(PackageInfo pkgInfo) {
        PackageManager packageManager = getPackageManager();
        CharSequence name = packageManager.getApplicationLabel(pkgInfo.applicationInfo);
        if (name != null) {
            return name.toString();
        } else {
            return pkgInfo.packageName;
        }
    }

    private void showTaskKillerDialogIfNecessary() {
        final List<PackageInfo> taskKillers = getInstalledTaskKillers();
        if (!shouldShowTaskKillerDialog(taskKillers)) {
            return;
        }

        // Build dialog content (note that the list is definitely not empty
        // so we don't need to check for the empty list edge case here)
        StringBuilder sb = new StringBuilder();
        for (PackageInfo pkgInfo : taskKillers) {
            sb.append(getAppDisplayName(pkgInfo));
            sb.append('\n');
        }
        sb.setLength(sb.length() - 1);
        String message = getString(R.string.task_killer_message, sb.toString());

        new AlertDialog.Builder(this)
            .setTitle(R.string.task_killer_title)
            .setMessage(message)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.task_killer_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    HashSet<String> knownTaskKillers = new HashSet<>();
                    for (PackageInfo pkgInfo : taskKillers) {
                        knownTaskKillers.add(pkgInfo.packageName);
                    }
                    mInternalPrefs.edit().putStringSet(PreferenceConsts.KEY_KNOWN_TASK_KILLERS, knownTaskKillers).apply();
                }
            })
            .setNegativeButton(R.string.ignore, null)
            .show();
    }

    private void showEnableModuleDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.enable_xposed_module_title)
            .setMessage(R.string.enable_xposed_module_message)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startXposedActivity(XposedUtils.Section.MODULES);
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
                    startXposedActivity(XposedUtils.Section.INSTALL);
                }
            })
            .setNegativeButton(R.string.ignore, null)
            .show();
    }

    private void showAboutDialog() {
        Spanned html = Html.fromHtml(getString(R.string.format_about_message,
            GITHUB_URL, WIKI_URL));

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name) + ' ' + VERSION_NAME)
            .setMessage(html)
            .setPositiveButton(R.string.close, null)
            .show();

        TextView textView = (TextView)dialog.findViewById(android.R.id.message);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
