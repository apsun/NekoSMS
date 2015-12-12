package com.crossbowffs.nekosms.app;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class ListRecyclerView extends RecyclerView {
    private final AdapterDataObserver mAdapterObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            updateEmptyView();
        }
    };

    private View mEmptyView;

    public ListRecyclerView(Context context) {
        super(context);
        addListDividers(context);
    }

    public ListRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        addListDividers(context);
    }

    public ListRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        addListDividers(context);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(mAdapterObserver);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(mAdapterObserver);
        }
        updateEmptyView();
    }

    public void setEmptyView(View emptyView) {
        mEmptyView = emptyView;
        updateEmptyView();
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    private void addListDividers(Context context) {
        addItemDecoration(new ListDividerDecoration(context));
    }

    private void updateEmptyView() {
        Adapter adapter = getAdapter();
        if (mEmptyView != null && adapter != null) {
            boolean emptyViewVisible = adapter.getItemCount() == 0;
            mEmptyView.setVisibility(emptyViewVisible ? VISIBLE : GONE);
            setVisibility(emptyViewVisible ? GONE : VISIBLE);
        }
    }
}
