package com.crossbowffs.nekosms.app;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.BroadcastConsts;
import com.crossbowffs.nekosms.data.SmsMessageData;
import com.crossbowffs.nekosms.database.BlockedSmsDbLoader;
import com.crossbowffs.nekosms.preferences.Preferences;
import com.crossbowffs.nekosms.provider.NekoSmsContract;

public class BlockedSmsListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private BlockedSmsListAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blockedsms_list);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        BlockedSmsListAdapter adapter = new BlockedSmsListAdapter(this);
        mAdapter = adapter;

        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);
        ListRecyclerView recyclerView = (ListRecyclerView)findViewById(R.id.activity_blockedsms_list_recyclerview);
        recyclerView.setAdapter(adapter);
        recyclerView.setEmptyView(findViewById(android.R.id.empty));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        showMessageDetailsDialog(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showMessageDetailsDialog(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        Preferences preferences = Preferences.fromContext(this);
        if (preferences.get(Preferences.PREF_DEBUG_MODE)) {
            inflater.inflate(R.menu.options_blockedsms_list, menu);
        }
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] from = {
            NekoSmsContract.Blocked._ID,
            NekoSmsContract.Blocked.SENDER,
            NekoSmsContract.Blocked.BODY,
            NekoSmsContract.Blocked.TIME_SENT,
            NekoSmsContract.Blocked.TIME_RECEIVED
        };
        Uri uri = NekoSmsContract.Blocked.CONTENT_URI;
        return new CursorLoader(this, uri, from, null, null, NekoSmsContract.Blocked.TIME_SENT + " DESC");
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

    private void showMessageDetailsDialog(Intent intent) {
        Uri uri = intent.getData();
        if (uri == null) {
            return;
        }

        SmsMessageData messageData = BlockedSmsDbLoader.loadMessage(this, uri);
        if (messageData != null) {
            mAdapter.showMessageDetailsDialog(messageData);
        } else {
            // This can occur if the user deletes the message, then opens the notification
            Toast.makeText(this, R.string.load_blocked_message_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void createTestSms() {
        SmsMessageData message = new SmsMessageData();
        message.setSender(getString(R.string.test_message_sender));
        message.setBody(getString(R.string.test_message_body));
        message.setTimeReceived(System.currentTimeMillis());
        message.setTimeSent(System.currentTimeMillis());

        Uri uri = BlockedSmsDbLoader.writeMessage(this, message);
        Intent intent = new Intent(BroadcastConsts.ACTION_RECEIVE_SMS);
        intent.putExtra(BroadcastConsts.EXTRA_MESSAGE, uri);
        sendBroadcast(intent, BroadcastConsts.PERMISSION_RECEIVE_SMS);
    }
}
