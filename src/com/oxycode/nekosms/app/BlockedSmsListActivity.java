package com.oxycode.nekosms.app;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toolbar;
import com.oxycode.nekosms.R;
import com.oxycode.nekosms.data.BlockedSmsLoader;
import com.oxycode.nekosms.data.SmsMessageData;
import com.oxycode.nekosms.provider.NekoSmsContract;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BlockedSmsListActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static class MessageListItemTag {
        public SmsMessageData mMessageData;
        public TextView mSenderTextView;
        public TextView mBodyTextView;
        public TextView mTimeSentTextView;
    }

    private static class BlockedSmsAdapter extends ResourceCursorAdapter {
        private int[] mColumns;

        public BlockedSmsAdapter(Context context) {
            super(context, R.layout.listitem_blockedsms_list, null, 0);
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

            tag.mSenderTextView.setText("Sender: " + messageData.getSender());
            tag.mBodyTextView.setText("Body: " + messageData.getBody());

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(messageData.getTimeSent());
            String timeSentStr = dateFormat.format(calendar.getTime());
            tag.mTimeSentTextView.setText("Sent at: " + timeSentStr);
        }
    }

    private BlockedSmsAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blockedsms_list);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setActionBar(toolbar);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);

        BlockedSmsAdapter adapter = new BlockedSmsAdapter(this);
        setListAdapter(adapter);
        mAdapter = adapter;

        registerForContextMenu(getListView());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
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
}
