package com.crossbowffs.nekosms.app;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.*;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.view.*;
import android.widget.*;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.BlockedSmsLoader;
import com.crossbowffs.nekosms.data.InboxSmsLoader;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.provider.NekoSmsContract;

public class BlockedSmsListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static class MessageListItemTag {
        public SmsMessageData mMessageData;
        public TextView mSenderTextView;
        public TextView mBodyTextView;
        public TextView mTimeSentTextView;
    }

    private static class BlockedSmsAdapter extends ResourceCursorAdapter {
        private Context mContext;
        private int[] mColumns;
        private String mSenderFormatString;
        private String mBodyFormatString;
        private String mTimeSentFormatString;

        public BlockedSmsAdapter(Context context) {
            super(context, R.layout.listitem_blockedsms_list, null, 0);

            mContext = context;
            Resources resources = context.getResources();
            mSenderFormatString = resources.getString(R.string.format_message_sender);
            mBodyFormatString = resources.getString(R.string.format_message_body);
            mTimeSentFormatString = resources.getString(R.string.format_message_time_sent);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);
            MessageListItemTag tag = new MessageListItemTag();
            tag.mSenderTextView = (TextView)view.findViewById(R.id.listitem_blockedsms_list_sender_textview);
            tag.mBodyTextView = (TextView)view.findViewById(R.id.listitem_blockedsms_list_body_textview);
            tag.mTimeSentTextView = (TextView)view.findViewById(R.id.listitem_blockedsms_list_timesent_textview);
            view.setTag(tag);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            if (mColumns == null) {
                mColumns = BlockedSmsLoader.getColumns(cursor);
            }

            MessageListItemTag tag = (MessageListItemTag)view.getTag();
            SmsMessageData messageData = BlockedSmsLoader.getMessageData(cursor, mColumns, tag.mMessageData);
            tag.mMessageData = messageData;

            String sender = messageData.getSender();
            String body = messageData.getBody();
            long timeSent = messageData.getTimeSent();
            String timeSentString = DateUtils.getRelativeTimeSpanString(mContext, timeSent).toString();

            tag.mSenderTextView.setText(String.format(mSenderFormatString, sender));
            tag.mBodyTextView.setText(String.format(mBodyFormatString, body));
            tag.mTimeSentTextView.setText(String.format(mTimeSentFormatString, timeSentString));
        }
    }

    private View mContentView;
    private BlockedSmsAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blockedsms_list);

        mContentView = findViewById(android.R.id.content);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        BlockedSmsAdapter adapter = new BlockedSmsAdapter(this);
        mAdapter = adapter;

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);

        ListView listView = (ListView)findViewById(android.R.id.list);
        listView.setEmptyView(findViewById(android.R.id.empty));
        listView.setAdapter(adapter);
        registerForContextMenu(listView);

        // TODO: implement interface on outer class
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showMessageDetailsDialog(id);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_blockedsms_list, menu);
        return super.onCreateOptionsMenu(menu);
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
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_blockedsms_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        long rowId = info.id;
        switch (item.getItemId()) {
        case R.id.contextmenu_blockedsms_list_restore:
            restoreSms(rowId);
            return true;
        case R.id.contextmenu_blockedsms_list_delete:
            deleteSms(rowId);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] from = {
            NekoSmsContract.Blocked._ID,
            NekoSmsContract.Blocked.SENDER,
            NekoSmsContract.Blocked.BODY,
            NekoSmsContract.Blocked.TIME_SENT,
            NekoSmsContract.Blocked.TIME_RECEIVED
        };
        Uri uri = NekoSmsContract.Blocked.CONTENT_URI;
        return new CursorLoader(this, uri, from, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    private void finishTryTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    private void createTestSms() {
        SmsMessageData message = new SmsMessageData();
        message.setSender("1234567890");
        message.setBody("This is a test");
        message.setTimeReceived(System.currentTimeMillis());
        message.setTimeSent(System.currentTimeMillis());
        BlockedSmsLoader.writeMessage(this, message);
    }

    private void restoreSms(long smsId) {
        final SmsMessageData messageData = BlockedSmsLoader.loadAndDeleteMessage(this, smsId);
        final Uri inboxSmsUri = InboxSmsLoader.writeMessage(this, messageData);
        Snackbar
            .make(mContentView, R.string.message_restored, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InboxSmsLoader.deleteMessage(BlockedSmsListActivity.this, inboxSmsUri);
                    BlockedSmsLoader.writeMessage(BlockedSmsListActivity.this, messageData);
                }
            })
            .show();
    }

    private void deleteSms(long smsId) {
        final SmsMessageData messageData = BlockedSmsLoader.loadAndDeleteMessage(this, smsId);
        Snackbar
            .make(mContentView, R.string.message_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BlockedSmsLoader.writeMessage(BlockedSmsListActivity.this, messageData);
                }
            })
            .show();
    }

    private void showMessageDetailsDialog(final long smsId) {
        Uri messageUri = ContentUris.withAppendedId(NekoSmsContract.Blocked.CONTENT_URI, smsId);
        SmsMessageData messageData = BlockedSmsLoader.loadMessage(this, messageUri);

        String sender = messageData.getSender();
        String body = messageData.getBody();
        long timeSent = messageData.getTimeSent();
        String escapedBody = Html.escapeHtml(body).replace("&#10;", "<br>");
        String timeSentString = DateUtils.getRelativeDateTimeString(
            this, timeSent, 0, DateUtils.WEEK_IN_MILLIS, 0).toString();

        Spanned html = Html.fromHtml(getString(R.string.format_message_details,
            sender, timeSentString, escapedBody));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
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
}
