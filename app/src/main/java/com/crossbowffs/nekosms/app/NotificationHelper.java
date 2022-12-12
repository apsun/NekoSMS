package com.crossbowffs.nekosms.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.consts.BroadcastConsts;
import com.crossbowffs.nekosms.consts.PreferenceConsts;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.loader.BlockedSmsLoader;

public final class NotificationHelper {
    private static final String NOTIFICATION_CHANNEL = "blocked_message";

    private NotificationHelper() { }

    private static int uriToNotificationId(Uri uri) {
        return (int)ContentUris.parseId(uri);
    }

    private static boolean areNotificationsEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return true;
        }

        SharedPreferences prefs = context.getSharedPreferences(PreferenceConsts.FILE_MAIN, Context.MODE_PRIVATE);
        return prefs.getBoolean(PreferenceConsts.KEY_NOTIFICATIONS_ENABLE, PreferenceConsts.KEY_NOTIFICATIONS_ENABLE_DEFAULT);
    }

    private static void applyNotificationStyle(Context context, Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PreferenceConsts.FILE_MAIN, Context.MODE_PRIVATE);
        String ringtone = prefs.getString(PreferenceConsts.KEY_NOTIFICATIONS_RINGTONE, PreferenceConsts.KEY_NOTIFICATIONS_RINGTONE_DEFAULT);
        if (!TextUtils.isEmpty(ringtone)) {
            notification.sound = Uri.parse(ringtone);
        }
        if (prefs.getBoolean(PreferenceConsts.KEY_NOTIFICATIONS_VIBRATE, PreferenceConsts.KEY_NOTIFICATIONS_VIBRATE_DEFAULT)) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        if (prefs.getBoolean(PreferenceConsts.KEY_NOTIFICATIONS_LIGHTS, PreferenceConsts.KEY_NOTIFICATIONS_LIGHTS_DEFAULT)) {
            notification.defaults |= Notification.DEFAULT_LIGHTS;
        }
        String priority = prefs.getString(PreferenceConsts.KEY_NOTIFICATIONS_PRIORITY, PreferenceConsts.KEY_NOTIFICATIONS_PRIORITY_DEFAULT);
        notification.priority = Integer.parseInt(priority);
    }

    private static PendingIntent createPendingIntent(Context context, String action, Uri uri) {
        Intent intent = new Intent(context, BlockedSmsReceiver.class);
        intent.setAction(action);
        intent.setData(uri);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getBroadcast(context, 0, intent, 0);
        }
    }

    private static Notification buildNotificationSingle(Context context, SmsMessageData messageData) {
        Uri uri = messageData.getUri();

        Intent viewIntent = new Intent(context, MainActivity.class);
        viewIntent.setAction(Intent.ACTION_VIEW);
        viewIntent.setData(uri);
        PendingIntent viewPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            viewPendingIntent = PendingIntent.getActivity(context, 0, viewIntent, 0);
        }

        PendingIntent deleteIntent = createPendingIntent(context, BroadcastConsts.ACTION_DELETE_SMS, uri);
        PendingIntent restoreIntent = createPendingIntent(context, BroadcastConsts.ACTION_RESTORE_SMS, uri);
        PendingIntent dismissIntent = createPendingIntent(context, BroadcastConsts.ACTION_DISMISS_NOTIFICATION, uri);

        return new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_blocked_message_24dp)
            .setContentTitle(context.getString(R.string.format_notification_single_sender, messageData.getSender()))
            .setContentText(messageData.getBody())
            .setStyle(new NotificationCompat.BigTextStyle().bigText(messageData.getBody()))
            .setContentIntent(viewPendingIntent)
            .addAction(R.drawable.ic_delete_24dp, context.getString(R.string.delete), deleteIntent)
            .addAction(R.drawable.ic_unarchive_24dp, context.getString(R.string.restore), restoreIntent)
            .setDeleteIntent(dismissIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(context, R.color.notification_bg))
            .build();
    }

    public static void cancelNotification(Context context, long messageId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel((int)messageId);
    }

    public static void cancelNotification(Context context, Uri messageUri) {
        cancelNotification(context, uriToNotificationId(messageUri));
    }

    public static void cancelAllNotifications(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancelAll();
    }

    public static void displayNotification(Context context, Uri messageUri) {
        if (!areNotificationsEnabled(context)) {
            BlockedSmsLoader.get().setSeenStatus(context, messageUri, true);
            return;
        }

        SmsMessageData messageData = BlockedSmsLoader.get().query(context, messageUri);
        Notification notification = buildNotificationSingle(context, messageData);
        applyNotificationStyle(context, notification);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(uriToNotificationId(messageUri), notification);
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        String name = context.getString(R.string.channel_blocked_messages);
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL, name, NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);
    }
}
