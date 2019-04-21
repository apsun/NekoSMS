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
import com.crossbowffs.nekosms.consts.BroadcastConsts;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.loader.BlockedSmsLoader;
import com.crossbowffs.nekosms.loader.DatabaseException;
import com.crossbowffs.nekosms.loader.InboxSmsLoader;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.utils.XposedUtils;
import com.crossbowffs.nekosms.widget.ListRecyclerView;

public class BlockedMessagesFragment extends MainFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final boolean DEBUG_MODE = BuildConfig.DEBUG;
    public static final String ARG_MESSAGE_URI = "message_uri";

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
        registerForContextMenu(mRecyclerView);
        disableFab();
        setTitle(R.string.blocked_messages);
        onNewArguments(getArguments());
        BlockedSmsLoader.get().markAllSeen(getContext());
    }

    @Override
    public void onNewArguments(Bundle args) {
        if (args == null) {
            return;
        }

        Uri messageUri = args.getParcelable(ARG_MESSAGE_URI);
        if (messageUri != null) {
            args.remove(ARG_MESSAGE_URI);
            showMessageDetailsDialog(messageUri);
            BlockedSmsLoader.get().markAllSeen(getContext());
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_blocked_messages, menu);
        menu.setHeaderTitle(R.string.message_actions);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListRecyclerView.ContextMenuInfo info = (ListRecyclerView.ContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.menu_item_restore_message:
            restoreSms(info.mId);
            return true;
        case R.id.menu_item_delete_message:
            deleteSms(info.mId);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
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
        return new CursorLoader(
            getContext(),
            DatabaseContract.BlockedMessages.CONTENT_URI,
            DatabaseContract.BlockedMessages.ALL, null, null,
            DatabaseContract.BlockedMessages.TIME_SENT + " DESC"
        );
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

        NotificationHelper.cancelAllNotifications(context);
        BlockedSmsLoader.get().deleteAll(context);
        showSnackbar(R.string.cleared_blocked_messages);
    }

    private void showConfirmClearDialog() {
        Context context = getContext();
        if (context == null) return;

        new AlertDialog.Builder(context)
            .setIcon(R.drawable.ic_warning_white_24dp)
            .setTitle(R.string.confirm_clear_messages_title)
            .setMessage(R.string.confirm_clear_messages_message)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    clearAllMessages();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showMessageDetailsDialog(Uri uri) {
        Context context = getContext();
        SmsMessageData messageData = BlockedSmsLoader.get().query(context, uri);
        if (messageData != null) {
            showMessageDetailsDialog(messageData);
        } else {
            // This can occur if the user deletes the message, then opens the notification
            showSnackbar(R.string.load_message_failed);
        }
    }

    public void showMessageDetailsDialog(final SmsMessageData messageData) {
        Context context = getContext();
        if (context == null) return;

        // Dismiss notification if present
        NotificationHelper.cancelNotification(context, messageData.getId());

        final long smsId = messageData.getId();
        String sender = messageData.getSender();
        String body = messageData.getBody();
        long timeSent = messageData.getTimeSent();
        String escapedBody = Html.escapeHtml(body).replace("&#10;", "<br>");
        String timeSentString = DateUtils.getRelativeDateTimeString(context, timeSent, 0, DateUtils.WEEK_IN_MILLIS, 0).toString();
        Spanned html = Html.fromHtml(getString(R.string.format_message_details, sender, timeSentString, escapedBody));

        new AlertDialog.Builder(context)
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
            })
            .show();

        BlockedSmsLoader.get().setReadStatus(context, messageData.getId(), true);
    }

    private void startXposedActivity(XposedUtils.Section section) {
        Context context = getContext();
        if (context == null) return;

        if (!XposedUtils.startXposedActivity(context, section)) {
            showSnackbar(R.string.xposed_not_installed);
        }
    }

    private void restoreSms(long smsId) {
        Context context = getContext();
        if (context == null) return;

        // We've obviously seen the message, so remove the notification
        NotificationHelper.cancelNotification(context, smsId);

        // Load message content (so we can undo)
        final SmsMessageData messageData = BlockedSmsLoader.get().query(context, smsId);
        if (messageData == null) {
            Xlog.e("Failed to restore message: could not load data");
            showSnackbar(R.string.load_message_failed);
            return;
        }

        // Write message to the inbox
        final Uri inboxSmsUri;
        try {
            inboxSmsUri = InboxSmsLoader.writeMessage(context, messageData);
        } catch (SecurityException e) {
            Xlog.e("Do not have permissions to write SMS");
            showSnackbar(R.string.must_enable_xposed_module, R.string.enable, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startXposedActivity(XposedUtils.Section.MODULES);
                }
            });
            return;
        } catch (DatabaseException e) {
            Xlog.e("Failed to restore message: could not write to SMS inbox");
            showSnackbar(R.string.message_restore_failed);
            return;
        }

        // Delete the message after we successfully write it to the inbox
        BlockedSmsLoader.get().delete(context, smsId);

        showSnackbar(R.string.message_restored, R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context2 = getContext();
                if (context2 == null) return;
                BlockedSmsLoader.get().insert(context2, messageData);
                InboxSmsLoader.deleteMessage(context2, inboxSmsUri);
            }
        });
    }

    private void deleteSms(long smsId) {
        Context context = getContext();
        if (context == null) return;

        // We've obviously seen the message, so remove the notification
        NotificationHelper.cancelNotification(context, smsId);

        // Load message content (for undo), then delete it
        final SmsMessageData messageData = BlockedSmsLoader.get().queryAndDelete(context, smsId);
        if (messageData == null) {
            Xlog.e("Failed to delete message: could not load data");
            showSnackbar(R.string.load_message_failed);
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
        message.setSender("+11234567890");
        message.setBody("Thanks for signing up for Cat Facts! You will now receive fun daily facts about CATS! >o<");
        message.setTimeReceived(System.currentTimeMillis());
        message.setTimeSent(System.currentTimeMillis());
        message.setRead(false);
        message.setSeen(false);

        Uri uri = BlockedSmsLoader.get().insert(context, message);
        Intent intent = new Intent(BroadcastConsts.ACTION_RECEIVE_SMS);
        intent.setComponent(new ComponentName(context, BroadcastConsts.RECEIVER_NAME));
        intent.putExtra(BroadcastConsts.EXTRA_MESSAGE, uri);
        context.sendBroadcast(intent);
    }
}
