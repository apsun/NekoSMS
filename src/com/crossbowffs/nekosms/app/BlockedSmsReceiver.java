package com.crossbowffs.nekosms.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.Toast;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.loader.BlockedSmsLoader;
import com.crossbowffs.nekosms.loader.CursorWrapper;
import com.crossbowffs.nekosms.loader.DatabaseException;
import com.crossbowffs.nekosms.loader.InboxSmsLoader;
import com.crossbowffs.nekosms.preferences.PrefConsts;
import com.crossbowffs.nekosms.preferences.PrefManager;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.AppOpsUtils;
import com.crossbowffs.nekosms.utils.Xlog;

public class BlockedSmsReceiver extends BroadcastReceiver {
    private static final String TAG = BlockedSmsReceiver.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1;

    private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private Notification buildNotificationSingle(Context context, SmsMessageData messageData) {
        Uri uri = ContentUris.withAppendedId(DatabaseContract.BlockedMessages.CONTENT_URI, messageData.getId());

        Intent viewIntent = new Intent(context, MainActivity.class);
        viewIntent.setAction(MainActivity.ACTION_OPEN_SECTION);
        viewIntent.putExtra(MainActivity.EXTRA_SECTION, MainActivity.EXTRA_SECTION_BLOCKED_MESSAGES);
        viewIntent.setData(uri);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);

        Intent deleteIntent = new Intent(context, BlockedSmsReceiver.class);
        deleteIntent.setAction(BroadcastConsts.ACTION_DELETE_SMS);
        deleteIntent.setData(uri);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, 0);

        Intent restoreIntent = new Intent(context, BlockedSmsReceiver.class);
        restoreIntent.setAction(BroadcastConsts.ACTION_RESTORE_SMS);
        restoreIntent.setData(uri);
        PendingIntent restorePendingIntent = PendingIntent.getBroadcast(context, 0, restoreIntent, 0);

        Intent dismissIntent = new Intent(context, BlockedSmsReceiver.class);
        dismissIntent.setAction(BroadcastConsts.ACTION_DISMISS_NOTIFICATION);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 0, dismissIntent, 0);

        Notification notification = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_message_blocked_white_24dp)
            .setContentTitle(context.getString(R.string.format_notification_single_sender, messageData.getSender()))
            .setContentText(messageData.getBody())
            .setStyle(new NotificationCompat.BigTextStyle().bigText(messageData.getBody()))
            .setContentIntent(viewPendingIntent)
            .addAction(R.drawable.ic_delete_white_24dp, context.getString(R.string.delete), deletePendingIntent)
            .addAction(R.drawable.ic_unarchive_white_24dp, context.getString(R.string.restore), restorePendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build();

        return notification;
    }

    private Notification buildNotificationMulti(Context context, CursorWrapper<SmsMessageData> messages) {
        Intent viewIntent = new Intent(context, MainActivity.class);
        viewIntent.setAction(MainActivity.ACTION_OPEN_SECTION);
        viewIntent.putExtra(MainActivity.EXTRA_SECTION, MainActivity.EXTRA_SECTION_BLOCKED_MESSAGES);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);

        Intent dismissIntent = new Intent(context, BlockedSmsReceiver.class);
        dismissIntent.setAction(BroadcastConsts.ACTION_DISMISS_NOTIFICATION);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 0, dismissIntent, 0);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        SmsMessageData data = new SmsMessageData();
        Spanned firstLine = null;
        while (messages.moveToNext()) {
            data = messages.get(data);
            String escapedSender = Html.escapeHtml(data.getSender().replace('\n', ' '));
            String escapedBody = Html.escapeHtml(data.getBody().replace('\n', ' '));
            Spanned line = Html.fromHtml(context.getString(R.string.format_notification_multi_item, escapedSender, escapedBody));
            if (firstLine == null) {
                firstLine = line;
            }
            inboxStyle.addLine(line);
        }
        inboxStyle.setSummaryText(context.getString(R.string.view_in_nekosms));

        Notification notification = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_message_blocked_white_24dp)
            .setContentTitle(context.getString(R.string.format_notification_multi_count, messages.getCount()))
            .setContentText(firstLine)
            .setStyle(inboxStyle)
            .setNumber(messages.getCount())
            .setContentIntent(viewPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build();

        return notification;
    }

    private void displayNotification(Context context, Intent intent) {
        PrefManager preferences = PrefManager.fromContext(context, PrefConsts.FILE_MAIN);
        if (!preferences.getBoolean(PrefManager.PREF_NOTIFICATIONS_ENABLE)) {
            // Just mark the message as seen and return
            Uri messageUri = intent.getParcelableExtra(BroadcastConsts.EXTRA_MESSAGE);
            BlockedSmsLoader.get().setSeenStatus(context, messageUri, true);
            return;
        }

        Notification notification;
        try (CursorWrapper<SmsMessageData> messages = BlockedSmsLoader.get().queryUnseen(context)) {
            if (messages == null || messages.getCount() == 0) {
                Xlog.e(TAG, "Failed to load read messages, falling back to intent URI");
                Uri messageUri = intent.getParcelableExtra(BroadcastConsts.EXTRA_MESSAGE);
                SmsMessageData messageData = BlockedSmsLoader.get().query(context, messageUri);
                if (messageData == null) {
                    Xlog.e(TAG, "Failed to load message from intent URI");
                    return;
                }
                notification = buildNotificationSingle(context, messageData);
            } else if (messages.getCount() == 1) {
                messages.moveToNext();
                notification = buildNotificationSingle(context, messages.get());
            } else {
                notification = buildNotificationMulti(context, messages);
            }
        }

        NotificationManager notificationManager = getNotificationManager(context);
        String ringtone = preferences.getString(PrefManager.PREF_NOTIFICATIONS_RINGTONE);
        if (!TextUtils.isEmpty(ringtone)) {
            notification.sound = Uri.parse(ringtone);
        }
        if (preferences.getBoolean(PrefManager.PREF_NOTIFICATIONS_VIBRATE)) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        if (preferences.getBoolean(PrefManager.PREF_NOTIFICATIONS_LIGHTS)) {
            notification.defaults |= Notification.DEFAULT_LIGHTS;
        }
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void onReceiveSms(Context context, Intent intent) {
        displayNotification(context, intent);
    }

    private void onDeleteSms(Context context, Intent intent) {
        NotificationManager notificationManager = getNotificationManager(context);
        notificationManager.cancel(NOTIFICATION_ID);

        Uri messageUri = intent.getData();
        boolean deleted = BlockedSmsLoader.get().delete(context, messageUri);
        if (!deleted) {
            Xlog.e(TAG, "Failed to delete message: could not load data");
            Toast.makeText(context, R.string.message_delete_failed, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.message_deleted, Toast.LENGTH_SHORT).show();
        }
    }

    private void onRestoreSms(Context context, Intent intent) {
        NotificationManager notificationManager = getNotificationManager(context);
        notificationManager.cancel(NOTIFICATION_ID);

        if (!AppOpsUtils.noteOp(context, AppOpsUtils.OP_WRITE_SMS)) {
            Xlog.e(TAG, "Do not have permissions to write SMS");
            Toast.makeText(context, R.string.must_enable_xposed_module, Toast.LENGTH_SHORT).show();
            return;
        }

        SmsMessageData messageToRestore = BlockedSmsLoader.get().query(context, intent.getData());
        if (messageToRestore == null) {
            Xlog.e(TAG, "Failed to restore message: could not load data");
            Toast.makeText(context, R.string.message_restore_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            InboxSmsLoader.writeMessage(context, messageToRestore);
        } catch (DatabaseException e) {
            Xlog.e(TAG, "Failed to restore message: could not write to SMS inbox");
            Toast.makeText(context, R.string.message_restore_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri messageUri = intent.getData();
        BlockedSmsLoader.get().delete(context, messageUri);
        Toast.makeText(context, R.string.message_restored, Toast.LENGTH_SHORT).show();
    }

    private void onDismissNotification(Context context, Intent intent) {
        // When the notification is dismissed, mark all messages as seen.
        // Technically we should only mark messages in the notification as
        // seen, but unless there is a race condition, the notification will
        // always contain all unseen messages.
        BlockedSmsLoader.get().markAllSeen(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
        case BroadcastConsts.ACTION_RECEIVE_SMS:
            onReceiveSms(context, intent);
            break;
        case BroadcastConsts.ACTION_DELETE_SMS:
            onDeleteSms(context, intent);
            break;
        case BroadcastConsts.ACTION_RESTORE_SMS:
            onRestoreSms(context, intent);
            break;
        case BroadcastConsts.ACTION_DISMISS_NOTIFICATION:
            onDismissNotification(context, intent);
            break;
        }
    }
}
