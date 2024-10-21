package com.crossbowffs.nekosms.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.consts.BroadcastConsts;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.loader.BlockedSmsLoader;
import com.crossbowffs.nekosms.loader.DatabaseException;
import com.crossbowffs.nekosms.loader.InboxSmsLoader;
import com.crossbowffs.nekosms.utils.Xlog;

public class BlockedSmsReceiver extends BroadcastReceiver {
    private void onReceiveSms(Context context, Intent intent) {
        Uri messageUri = intent.getParcelableExtra(BroadcastConsts.EXTRA_MESSAGE);
        if (messageUri == null) {
            return;
        }

        NotificationHelper.displayNotification(context, messageUri);
    }

    private void onDeleteSms(Context context, Intent intent) {
        Uri messageUri = intent.getData();
        NotificationHelper.cancelNotification(context, messageUri);

        boolean deleted = BlockedSmsLoader.get().delete(context, messageUri);
        if (!deleted) {
            Xlog.e("Failed to delete message: could not load data");
            Toast.makeText(context, R.string.load_message_failed, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.message_deleted, Toast.LENGTH_SHORT).show();
        }
    }

    private void onRestoreSms(Context context, Intent intent) {
        Uri messageUri = intent.getData();
        NotificationHelper.cancelNotification(context, messageUri);

        // Always mark message as seen, even though we're deleting it,
        // so even if we don't get to delete it, the seen flag still gets set.
        // This is a no-op if the message doesn't exist.
        BlockedSmsLoader.get().setSeenStatus(context, messageUri, true);

        SmsMessageData messageToRestore = BlockedSmsLoader.get().query(context, messageUri);
        if (messageToRestore == null) {
            Xlog.e("Failed to restore message: could not load data");
            Toast.makeText(context, R.string.load_message_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            InboxSmsLoader.writeMessage(context, messageToRestore);
        } catch (SecurityException e) {
            Xlog.e("Do not have permissions to write SMS");
            Toast.makeText(context, R.string.must_enable_xposed_module, Toast.LENGTH_SHORT).show();
            return;
        } catch (DatabaseException e) {
            Xlog.e("Failed to restore message: could not write to SMS inbox");
            Toast.makeText(context, R.string.message_restore_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        BlockedSmsLoader.get().delete(context, messageUri);
        Toast.makeText(context, R.string.message_restored, Toast.LENGTH_SHORT).show();
    }

    private void onDismissNotification(Context context, Intent intent) {
        Uri messageUri = intent.getData();
        BlockedSmsLoader.get().setSeenStatus(context, messageUri, true);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }

        switch (action) {
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
