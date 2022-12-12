package com.crossbowffs.nekosms.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.consts.PreferenceConsts;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.IOUtils;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SECTION = "section";
    public static final String EXTRA_SECTION_BLACKLIST_RULES = "blacklist_rules";
    public static final String EXTRA_SECTION_WHITELIST_RULES = "whitelist_rules";
    public static final String EXTRA_SECTION_BLOCKED_MESSAGES = "blocked_messages";
    public static final String EXTRA_SECTION_SETTINGS = "settings";

    private static final int VERSION_CODE = BuildConfig.VERSION_CODE;

    private BottomNavigationView mBottomNavBar;
    private FloatingActionButton mFloatingActionButton;
    private Set<Snackbar> mSnackbars;
    private Fragment mContentFragment;
    private String mContentSection;
    private SharedPreferences mInternalPrefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFloatingActionButton = findViewById(R.id.main_fab);
        mFloatingActionButton.hide();

        mBottomNavBar = findViewById(R.id.bottom_nav);
        mBottomNavBar.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
            case R.id.bottom_nav_blacklist:
                setContentSection(EXTRA_SECTION_BLACKLIST_RULES);
                return true;
            case R.id.bottom_nav_whitelist:
                setContentSection(EXTRA_SECTION_WHITELIST_RULES);
                return true;
            case R.id.bottom_nav_blocked:
                setContentSection(EXTRA_SECTION_BLOCKED_MESSAGES);
                return true;
            case R.id.bottom_nav_settings:
                setContentSection(EXTRA_SECTION_SETTINGS);
                return true;
            default:
                return false;
            }
        });

        // Load preferences
        mInternalPrefs = getSharedPreferences(PreferenceConsts.FILE_INTERNAL, MODE_PRIVATE);
        mInternalPrefs.edit().putInt(PreferenceConsts.KEY_APP_VERSION, VERSION_CODE).apply();

        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));

        // This is used to cache displayed snackbars, so we can
        // dismiss them when switching between fragments.
        mSnackbars = Collections.newSetFromMap(new WeakHashMap<>());

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
            }

            // Set the section that was selected previously
            String section = mInternalPrefs.getString(PreferenceConsts.KEY_SELECTED_SECTION, EXTRA_SECTION_BLACKLIST_RULES);
            setContentSection(section);
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
                    args.putParcelable(SettingsFragment.ARG_IMPORT_URI, uri);
                    setContentSection(EXTRA_SECTION_SETTINGS, args);
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
                if (mContentFragment instanceof OnNewArgumentsListener) {
                    ((OnNewArgumentsListener)mContentFragment).onNewArguments(args);
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
            navId = R.id.bottom_nav_blacklist;
            break;
        case EXTRA_SECTION_WHITELIST_RULES:
            fragment = new FilterRulesFragment();
            if (args == null) {
                args = new Bundle();
            }
            args.putString(FilterRulesFragment.EXTRA_ACTION, SmsFilterAction.ALLOW.name());
            navId = R.id.bottom_nav_whitelist;
            break;
        case EXTRA_SECTION_BLOCKED_MESSAGES:
            fragment = new BlockedMessagesFragment();
            navId = R.id.bottom_nav_blocked;
            break;
        case EXTRA_SECTION_SETTINGS:
            fragment = new SettingsFragment();
            navId = R.id.bottom_nav_settings;
            break;
        default:
            Xlog.e("Unknown context section: %s", key);
            return setContentSection(EXTRA_SECTION_BLACKLIST_RULES);
        }

        if (args != null) {
            fragment.setArguments(args);
        }
        disableFab();
        dismissSnackbar();
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.main_content, fragment)
            .commit();
        mContentFragment = fragment;
        mContentSection = key;
        mBottomNavBar.setSelectedItemId(navId);
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
        Snackbar snackbar = Snackbar.make(mFloatingActionButton, textId, Snackbar.LENGTH_LONG);
        if (mFloatingActionButton.getVisibility() == View.VISIBLE) {
            snackbar.setAnchorView(mFloatingActionButton);
        }
        mSnackbars.add(snackbar);
        return snackbar;
    }

    public void dismissSnackbar() {
        // Items will be automatically removed from the cache
        // once the references are GC'd
        for (Snackbar snackbar : mSnackbars) {
            // Hack to prevent FAB from jumping around when switching tabs
            snackbar.getView().setVisibility(View.GONE);

            snackbar.dismiss();
        }
    }

    private void startXposedActivity(XposedUtils.Section section) {
        if (!XposedUtils.startXposedActivity(this, section)) {
            makeSnackbar(R.string.xposed_not_installed).show();
        }
    }

    private void showEnableModuleDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.enable_xposed_module_title)
            .setMessage(R.string.enable_xposed_module_message)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.enable, (dialog, which) -> {
                startXposedActivity(XposedUtils.Section.MODULES);
            })
            .setNegativeButton(R.string.ignore, null)
            .show();
    }

    private void showModuleUpdatedDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.module_outdated_title)
            .setMessage(R.string.module_outdated_message)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setPositiveButton(R.string.reboot, (dialog, which) -> {
                startXposedActivity(XposedUtils.Section.INSTALL);
            })
            .setNegativeButton(R.string.ignore, null)
            .show();
    }

    public static MainActivity from(Fragment fragment) {
        return (MainActivity)fragment.getActivity();
    }
}
