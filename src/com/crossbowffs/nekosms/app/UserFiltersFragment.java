package com.crossbowffs.nekosms.app;

import android.app.LoaderManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.loader.UserRuleLoader;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.Xlog;

public class UserFiltersFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private class ScrollListener extends RecyclerView.OnScrollListener {
        private static final int SHOW_THRESHOLD = 50;
        private static final int HIDE_THRESHOLD = 100;
        private int mScrollDistance = 0;
        private boolean mControlsVisible = true;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if ((mControlsVisible && dy > 0) || (!mControlsVisible && dy < 0)) {
                mScrollDistance += dy;
            }

            if (mControlsVisible && mScrollDistance > HIDE_THRESHOLD) {
                mControlsVisible = false;
                mScrollDistance = 0;
                scrollFabOut();
            } else if (!mControlsVisible && mScrollDistance < -SHOW_THRESHOLD) {
                mControlsVisible = true;
                mScrollDistance = 0;
                scrollFabIn();
            }
        }
    }

    private static final String TAG = UserFiltersFragment.class.getSimpleName();

    private ListRecyclerView mFilterListView;
    private View mEmptyView;
    private UserFiltersAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context darkTheme = new ContextThemeWrapper(getActivity(), R.style.AppTheme);
        LayoutInflater localInflater = inflater.cloneInContext(darkTheme);
        View view = localInflater.inflate(R.layout.fragment_filter_list, container, false);
        mFilterListView = (ListRecyclerView)view.findViewById(R.id.activity_filter_list_recyclerview);
        mEmptyView = view.findViewById(android.R.id.empty);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        UserFiltersAdapter adapter = new UserFiltersAdapter(this);
        mAdapter = adapter;
        LoaderManager loaderManager = getLoaderManager();
        loaderManager.initLoader(0, null, this);
        mFilterListView.setAdapter(adapter);
        mFilterListView.setEmptyView(mEmptyView);
        mFilterListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mFilterListView.addOnScrollListener(new ScrollListener());
        registerForContextMenu(mFilterListView);
        setFabVisible(true);
        setFabCallback(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), FilterEditorActivity.class);
                startActivity(intent);
            }
        });
        setTitle(R.string.user_filters);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_filter_list, menu);
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.options_filter_list, menu);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getContext(),
            NekoSmsContract.FilterListRules.CONTENT_URI,
            NekoSmsContract.FilterListRules.ALL, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.changeCursor(null);
    }

    private void deleteFilter(long filterId) {
        final SmsFilterData filterData = UserRuleLoader.get().queryAndDelete(getContext(), filterId);
        if (filterData == null) {
            Xlog.e(TAG, "Failed to delete filter: could not load data");
            return;
        }

        showSnackbar(R.string.filter_deleted, R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserRuleLoader.get().insert(getContext(), filterData);
            }
        });
    }

    public void startFilterEditorActivity(long id) {
        Intent intent = new Intent(getContext(), FilterEditorActivity.class);
        Uri filterUri = ContentUris.withAppendedId(NekoSmsContract.FilterListRules.CONTENT_URI, id);
        intent.setData(filterUri);
        startActivity(intent);
    }
}
