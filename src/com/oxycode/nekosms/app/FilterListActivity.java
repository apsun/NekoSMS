package com.oxycode.nekosms.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toolbar;
import com.oxycode.nekosms.R;
import com.oxycode.nekosms.provider.NekoSmsContract;

public class FilterListActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_list);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_filter_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_create_filter:
                // TODO: Testing code, remove when done
                ContentValues v = new ContentValues();
                v.put(NekoSmsContract.Filters.PRIORITY, 0);
                v.put(NekoSmsContract.Filters.ACTION, "BLOCK");
                v.put(NekoSmsContract.Filters.FIELD, "BODY");
                v.put(NekoSmsContract.Filters.MODE, "REGEX");
                v.put(NekoSmsContract.Filters.PATTERN, "^123");
                getContentResolver().insert(NekoSmsContract.Filters.CONTENT_URI, v);

                // Intent filterEditorIntent = new Intent(this, FilterEditorActivity.class);
                // startActivity(filterEditorIntent);
                return true;
            case R.id.menu_item_import_from_sms:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
