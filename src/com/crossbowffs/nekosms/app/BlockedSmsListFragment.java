package com.crossbowffs.nekosms.app;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.view.*;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.BroadcastConsts;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.database.BlockedSmsDbLoader;
import com.crossbowffs.nekosms.database.DatabaseException;
import com.crossbowffs.nekosms.database.InboxSmsDbLoader;
import com.crossbowffs.nekosms.preferences.PrefKeys;
import com.crossbowffs.nekosms.preferences.PrefManager;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.AppOpsUtils;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;

public class BlockedSmsListFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = BlockedSmsListFragment.class.getSimpleName();

    private ListRecyclerView mBlockedSmsListView;
    private View mEmptyView;
    private BlockedSmsListAdapter mAdapter;
    private String mMessageDetailsFormatString;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blockedsms_list, container, false);
        mBlockedSmsListView = (ListRecyclerView)view.findViewById(R.id.activity_blockedsms_list_recyclerview);
        mEmptyView = view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new BlockedSmsListAdapter(this);
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);
        mBlockedSmsListView.setAdapter(mAdapter);
        mBlockedSmsListView.setEmptyView(mEmptyView);
        mBlockedSmsListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mMessageDetailsFormatString = getString(R.string.format_message_details);
        setFabVisible(false);
        setFabCallback(null);
        setTitle(R.string.blocked_messages);
        showMessageDetailsDialog(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        showMessageDetailsDialog(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        PrefManager preferences = PrefManager.fromContext(getContext(), PrefKeys.FILE_MAIN);
        if (preferences.getBoolean(PrefManager.PREF_DEBUG_MODE)) {
            inflater.inflate(R.menu.options_blockedsms_list_debug, menu);
        } else {
            inflater.inflate(R.menu.options_blockedsms_list, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finishTryTransition();
            return true;
        case R.id.menu_item_create_test:
            createTestSms();
            return true;
        case R.id.menu_item_clear_blocked:
            showConfirmClearDialog();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(),
            NekoSmsContract.Blocked.CONTENT_URI,
            NekoSmsContract.Blocked.ALL, null, null,
            NekoSmsContract.Blocked.TIME_SENT + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    private void clearAllMessages() {
        BlockedSmsDbLoader.deleteAllMessages(getContext());
        showToast(R.string.cleared_blocked_messages);
    }

    private void showConfirmClearDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setTitle(R.string.confirm_clear_messages_title)
            .setMessage(R.string.confirm_clear_messages_message)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    clearAllMessages();
                }
            })
            .setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showMessageDetailsDialog(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            return;
        } else {
            // TODO: Is this a good workaround? Essentially "consume" the URI data
            intent.setData(null);
        }

        SmsMessageData messageData = BlockedSmsDbLoader.loadMessage(getContext(), uri);
        if (messageData != null) {
            showMessageDetailsDialog(messageData);
        } else {
            // This can occur if the user deletes the message, then opens the notification
            showToast(R.string.load_blocked_message_failed);
        }
    }

    public void showMessageDetailsDialog(final SmsMessageData messageData) {
        final long smsId = messageData.getId();
        String sender = messageData.getSender();
        String body = messageData.getBody();
        long timeSent = messageData.getTimeSent();
        String escapedBody = Html.escapeHtml(body).replace("&#10;", "<br>");
        String timeSentString = DateUtils.getRelativeDateTimeString(
            getContext(), timeSent, 0, DateUtils.WEEK_IN_MILLIS, 0).toString();
        Spanned html = Html.fromHtml(String.format(
            mMessageDetailsFormatString, sender, timeSentString, escapedBody));

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
            .setMessage(html)
            .setNeutralButton(R.string.close, null)
            .setPositiveButton(R.string.restore, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    restoreSms(smsId);
                }
            })
            .setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteSms(smsId);
                }
            });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startXposedActivity(String section) {
        if (!XposedUtils.startXposedActivity(getContext(), section)) {
            showToast(R.string.xposed_not_installed);
        }
    }

    private void restoreSms(long smsId) {
        if (!AppOpsUtils.noteOp(getContext(), AppOpsUtils.OP_WRITE_SMS)) {
            Xlog.e(TAG, "Do not have permissions to write SMS");
            showSnackbar(R.string.must_enable_xposed_module, R.string.enable, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startXposedActivity(XposedUtils.XPOSED_SECTION_MODULES);
                }
            });
            return;
        }

        final SmsMessageData messageData = BlockedSmsDbLoader.loadMessage(getContext(), smsId);
        if (messageData == null) {
            Xlog.e(TAG, "Failed to restore message: could not load data");
            showToast(R.string.load_blocked_message_failed);
            return;
        }

        final Uri inboxSmsUri;
        try {
            inboxSmsUri = InboxSmsDbLoader.writeMessage(getContext(), messageData);
        } catch (DatabaseException e) {
            Xlog.e(TAG, "Failed to restore message: could not write to SMS inbox");
            showToast(R.string.message_restore_failed);
            return;
        }

        // Only delete the message after we have successfully written it to the SMS inbox
        BlockedSmsDbLoader.deleteMessage(getContext(), smsId);

        showSnackbar(R.string.message_restored, R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InboxSmsDbLoader.deleteMessage(getContext(), inboxSmsUri);
                BlockedSmsDbLoader.writeMessage(getContext(), messageData);
            }
        });
    }

    private void deleteSms(long smsId) {
        final SmsMessageData messageData = BlockedSmsDbLoader.loadAndDeleteMessage(getContext(), smsId);
        if (messageData == null) {
            Xlog.e(TAG, "Failed to delete message: could not load data");
            showToast(R.string.load_blocked_message_failed);
            return;
        }

        showSnackbar(R.string.message_deleted, R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BlockedSmsDbLoader.writeMessage(getContext(), messageData);
            }
        });
    }

    private void createTestSms() {
        SmsMessageData message = new SmsMessageData();
        message.setSender(getString(R.string.test_message_sender));
        message.setBody(getString(R.string.test_message_body));
        message.setTimeReceived(System.currentTimeMillis());
        message.setTimeSent(System.currentTimeMillis());

        Uri uri = BlockedSmsDbLoader.writeMessage(getContext(), message);
        Intent intent = new Intent(BroadcastConsts.ACTION_RECEIVE_SMS);
        intent.putExtra(BroadcastConsts.EXTRA_MESSAGE, uri);
        getContext().sendBroadcast(intent);
    }
}
