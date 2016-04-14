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
import android.text.TextUtils;
import android.widget.Toast;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.BroadcastConsts;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.database.BlockedSmsDbLoader;
import com.crossbowffs.nekosms.database.CursorWrapper;
import com.crossbowffs.nekosms.database.DatabaseException;
import com.crossbowffs.nekosms.database.InboxSmsDbLoader;
import com.crossbowffs.nekosms.preferences.PrefKeys;
import com.crossbowffs.nekosms.preferences.PrefManager;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.AppOpsUtils;
import com.crossbowffs.nekosms.utils.Xlog;

public class BlockedSmsReceiver extends BroadcastReceiver {
    private static final String TAG = BlockedSmsReceiver.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1;

    private NotificationManager getNotificationManager(Context context) {
        return (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private Notification buildNotificationSingle(Context context, SmsMessageData messageData) {
        Uri uri = ContentUris.withAppendedId(NekoSmsContract.Blocked.CONTENT_URI, messageData.getId());

        Intent viewIntent = new Intent(context, MainActivity.class);
        viewIntent.setAction(MainActivity.ACTION_OPEN_SECTION);
        viewIntent.putExtra(MainActivity.EXTRA_SECTION, MainActivity.EXTRA_SECTION_BLOCKED_SMS_LIST);
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

        Notification notification = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_message_blocked_white_24dp)
            .setContentTitle(context.getString(R.string.format_notification_single_sender, messageData.getSender()))
            .setContentText(messageData.getBody())
            .setStyle(new NotificationCompat.BigTextStyle().bigText(messageData.getBody()))
            .setContentIntent(viewPendingIntent)
            .addAction(R.drawable.ic_delete_white_24dp, context.getString(R.string.delete), deletePendingIntent)
            .addAction(R.drawable.ic_unarchive_white_24dp, context.getString(R.string.restore), restorePendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build();

        return notification;
    }

    private Notification buildNotificationMulti(Context context, CursorWrapper<SmsMessageData> messages) {
        Intent viewIntent = new Intent(context, MainActivity.class);
        viewIntent.setAction(MainActivity.ACTION_OPEN_SECTION);
        viewIntent.putExtra(MainActivity.EXTRA_SECTION, MainActivity.EXTRA_SECTION_BLOCKED_SMS_LIST);
        PendingIntent viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        SmsMessageData data = new SmsMessageData();
        String firstLine = null;
        while (messages.moveToNext()) {
            data = messages.get(data);
            String line = context.getString(R.string.format_notification_multi_item, data.getSender(), data.getBody());
            if (firstLine == null) {
                firstLine = line;
            }
            inboxStyle.addLine(line);
        }

        Notification notification = new NotificationCompat.Builder(context)
            .setSmallIcon(R.drawable.ic_message_blocked_white_24dp)
            .setContentTitle(context.getString(R.string.format_notification_multi_count, messages.getCount()))
            .setContentText(firstLine)
            .setStyle(inboxStyle)
            .setNumber(messages.getCount())
            .setContentIntent(viewPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .build();

        return notification;
    }

    private void displayNotification(Context context, Intent intent) {
        PrefManager preferences = PrefManager.fromContext(context, PrefKeys.FILE_MAIN);
        if (!preferences.getBoolean(PrefManager.PREF_NOTIFICATIONS_ENABLE)) {
            return;
        }

        CursorWrapper<SmsMessageData> messages = BlockedSmsDbLoader.loadAllMessages(context, true);
        Notification notification;
        if (messages == null || messages.getCount() == 0) {
            Xlog.e(TAG, "Failed to load read messages, falling back to intent URI");
            Uri messageUri = intent.getParcelableExtra(BroadcastConsts.EXTRA_MESSAGE);
            SmsMessageData messageData = BlockedSmsDbLoader.loadMessage(context, messageUri);
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
        boolean deleted = BlockedSmsDbLoader.deleteMessage(context, messageUri);
        if (!deleted) {
            Xlog.e(TAG, "Failed to delete message: could not load data");
            Toast.makeText(context, R.string.load_blocked_message_failed, Toast.LENGTH_SHORT).show();
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

        SmsMessageData messageToRestore = BlockedSmsDbLoader.loadMessage(context, intent.getData());
        if (messageToRestore == null) {
            Xlog.e(TAG, "Failed to restore message: could not load data");
            Toast.makeText(context, R.string.load_blocked_message_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            InboxSmsDbLoader.writeMessage(context, messageToRestore);
        } catch (DatabaseException e) {
            Xlog.e(TAG, "Failed to restore message: could not write to SMS inbox");
            Toast.makeText(context, R.string.message_restore_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri messageUri = intent.getData();
        BlockedSmsDbLoader.deleteMessage(context, messageUri);
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
        }
    }
}
