package com.crossbowffs.nekosms.app;

import android.app.LoaderManager;
import android.content.*;
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
import com.crossbowffs.nekosms.BuildConfig;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.loader.BlockedSmsLoader;
import com.crossbowffs.nekosms.loader.DatabaseException;
import com.crossbowffs.nekosms.loader.InboxSmsLoader;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.widget.ListRecyclerView;
import com.crossbowffs.nekosms.utils.AppOpsUtils;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;

public class BlockedMessagesFragment extends MainFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = BlockedMessagesFragment.class.getSimpleName();
    private static final boolean DEBUG_MODE = BuildConfig.DEBUG;

    private ListRecyclerView mRecyclerView;
    private View mEmptyView;
    private BlockedMessagesAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_blocked_messages, container, false);
        mRecyclerView = (ListRecyclerView)view.findViewById(R.id.blocked_messages_recyclerview);
        mEmptyView = view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new BlockedMessagesAdapter(this);
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setEmptyView(mEmptyView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        setFabVisible(false);
        setFabCallback(null);
        setTitle(R.string.blocked_messages);
        showMessageDetailsDialog(getIntent());
        BlockedSmsLoader.get().markAllSeen(getContext());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        showMessageDetailsDialog(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.options_blocked_messages, menu);
        if (DEBUG_MODE) {
            inflater.inflate(R.menu.options_debug, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
            DatabaseContract.BlockedMessages.CONTENT_URI,
            DatabaseContract.BlockedMessages.ALL, null, null,
            DatabaseContract.BlockedMessages.TIME_SENT + " DESC");
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
        Context context = getContext();
        if (context == null) return;

        BlockedSmsLoader.get().deleteAll(context);
        showToast(R.string.cleared_blocked_messages);
    }

    private void showConfirmClearDialog() {
        Context context = getContext();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
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
        Context context = getContext();
        if (context == null) return;

        Uri uri = intent.getData();
        if (uri == null) {
            return;
        } else {
            // Consume the URI data, so we don't re-show the dialog
            // when re-initializing the fragment
            intent.setData(null);
        }

        SmsMessageData messageData = BlockedSmsLoader.get().query(context, uri);
        if (messageData != null) {
            showMessageDetailsDialog(messageData);
        } else {
            // This can occur if the user deletes the message, then opens the notification
            showToast(R.string.load_blocked_message_failed);
        }
    }

    public void showMessageDetailsDialog(final SmsMessageData messageData) {
        Context context = getContext();
        if (context == null) return;

        final long smsId = messageData.getId();
        String sender = messageData.getSender();
        String body = messageData.getBody();
        long timeSent = messageData.getTimeSent();
        String escapedBody = Html.escapeHtml(body).replace("&#10;", "<br>");
        String timeSentString = DateUtils.getRelativeDateTimeString(context, timeSent, 0, DateUtils.WEEK_IN_MILLIS, 0).toString();
        Spanned html = Html.fromHtml(getString(R.string.format_message_details, sender, timeSentString, escapedBody));

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
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

        BlockedSmsLoader.get().setReadStatus(context, messageData.getId(), true);
    }

    private void startXposedActivity(String section) {
        Context context = getContext();
        if (context == null) return;
        if (!XposedUtils.startXposedActivity(context, section)) {
            showToast(R.string.xposed_not_installed);
        }
    }

    private void restoreSms(long smsId) {
        Context context = getContext();
        if (context == null) return;

        if (!AppOpsUtils.noteOp(context, AppOpsUtils.OP_WRITE_SMS)) {
            Xlog.e(TAG, "Do not have permissions to write SMS");
            showSnackbar(R.string.must_enable_xposed_module, R.string.enable, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startXposedActivity(XposedUtils.XPOSED_SECTION_MODULES);
                }
            });
            return;
        }

        final SmsMessageData messageData = BlockedSmsLoader.get().query(context, smsId);
        if (messageData == null) {
            Xlog.e(TAG, "Failed to restore message: could not load data");
            showToast(R.string.load_blocked_message_failed);
            return;
        }

        final Uri inboxSmsUri;
        try {
            inboxSmsUri = InboxSmsLoader.writeMessage(context, messageData);
        } catch (DatabaseException e) {
            Xlog.e(TAG, "Failed to restore message: could not write to SMS inbox");
            showToast(R.string.message_restore_failed);
            return;
        }

        // Only delete the message after we have successfully written it to the SMS inbox
        BlockedSmsLoader.get().delete(context, smsId);

        showSnackbar(R.string.message_restored, R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context2 = getContext();
                if (context2 == null) return;
                InboxSmsLoader.deleteMessage(context2, inboxSmsUri);
                BlockedSmsLoader.get().insert(context2, messageData);
            }
        });
    }

    private void deleteSms(long smsId) {
        Context context = getContext();
        if (context == null) return;
        final SmsMessageData messageData = BlockedSmsLoader.get().queryAndDelete(context, smsId);
        if (messageData == null) {
            Xlog.e(TAG, "Failed to delete message: could not load data");
            showToast(R.string.load_blocked_message_failed);
            return;
        }

        showSnackbar(R.string.message_deleted, R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context2 = getContext();
                if (context2 == null) return;
                BlockedSmsLoader.get().insert(context2, messageData);
            }
        });
    }

    private void createTestSms() {
        Context context = getContext();
        if (context == null) return;

        SmsMessageData message = new SmsMessageData();
        message.setSender("+12345678900");
        message.setBody(
            "First line\n" +
            "This is a test message with " +
            "loooooooooooooooooooooooooooooooooooooooooooo" +
            "ooooooooooooooooooooooooong content\n\n" +
            "This is a new line!");
        message.setTimeReceived(System.currentTimeMillis());
        message.setTimeSent(System.currentTimeMillis());
        message.setRead(false);
        message.setSeen(false);

        Uri uri = BlockedSmsLoader.get().insert(context, message);
        Intent intent = new Intent(BroadcastConsts.ACTION_RECEIVE_SMS);
        intent.putExtra(BroadcastConsts.EXTRA_MESSAGE, uri);
        context.sendBroadcast(intent);
    }
}
