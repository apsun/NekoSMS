package com.crossbowffs.nekosms.app;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;
import com.crossbowffs.nekosms.provider.DatabaseContract;
import com.crossbowffs.nekosms.utils.Xlog;
import com.crossbowffs.nekosms.widget.ListRecyclerView;

public class FilterRulesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_ACTION = "action";

    private ListRecyclerView mRecyclerView;
    private TextView mEmptyView;
    private FilterRulesAdapter mAdapter;
    private SmsFilterAction mAction;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAction = SmsFilterAction.parse(getArguments().getString(EXTRA_ACTION));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter_rules, container, false);
        mRecyclerView = view.findViewById(R.id.filter_rules_recyclerview);
        mEmptyView = view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize filter list
        mAdapter = new FilterRulesAdapter(this);
        LoaderManager loaderManager = LoaderManager.getInstance(this);
        loaderManager.initLoader(0, null, this);
        mRecyclerView.setEmptyView(mEmptyView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        registerForContextMenu(mRecyclerView);

        MainActivity activity = MainActivity.from(this);

        // Display create FAB
        activity.enableFab(R.drawable.ic_create_24dp, v -> {
            Intent intent = new Intent(getContext(), FilterEditorActivity.class);
            intent.putExtra(FilterEditorActivity.EXTRA_ACTION, mAction.name());
            startActivity(intent);
        });

        // Set strings according to which section we're displaying
        if (mAction == SmsFilterAction.BLOCK) {
            activity.setTitle(R.string.blacklist_rules);
            mEmptyView.setText(R.string.blacklist_rules_empty_text);
        } else if (mAction == SmsFilterAction.ALLOW) {
            activity.setTitle(R.string.whitelist_rules);
            mEmptyView.setText(R.string.whitelist_rules_empty_text);
        }
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_filter_rules, menu);
        menu.setHeaderTitle(R.string.filter_actions);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListRecyclerView.ContextMenuInfo info = (ListRecyclerView.ContextMenuInfo)item.getMenuInfo();
        switch (item.getItemId()) {
        case R.id.menu_item_edit_filter:
            startFilterEditorActivity(info.mId);
            return true;
        case R.id.menu_item_delete_filter:
            deleteFilter(info.mId);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
            getContext(),
            DatabaseContract.FilterRules.CONTENT_URI,
            DatabaseContract.FilterRules.ALL,
            DatabaseContract.FilterRules.ACTION + "=?",
            new String[] {mAction.name()},
            null
        );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
        if (mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(mAdapter);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
        mAdapter.changeCursor(null);
    }

    private void deleteFilter(long filterId) {
        final SmsFilterData filterData = FilterRuleLoader.get().queryAndDelete(getContext(), filterId);
        if (filterData == null) {
            Xlog.e("Failed to delete filter: could not load data");
            return;
        }

        MainActivity.from(this)
            .makeSnackbar(R.string.filter_deleted)
            .setAction(R.string.undo, v -> {
                FilterRuleLoader.get().insert(getContext(), filterData);
            })
            .show();
    }

    public void startFilterEditorActivity(long id) {
        Intent intent = new Intent(getContext(), FilterEditorActivity.class);
        Uri filterUri = ContentUris.withAppendedId(DatabaseContract.FilterRules.CONTENT_URI, id);
        intent.setData(filterUri);
        startActivity(intent);
    }
}
