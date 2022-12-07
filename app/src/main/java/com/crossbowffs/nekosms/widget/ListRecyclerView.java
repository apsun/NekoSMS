package com.crossbowffs.nekosms.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ListRecyclerView extends RecyclerView {
    private static class ListDividerDecoration extends ItemDecoration {
        private final Drawable mDivider;

        public ListDividerDecoration(Context context) {
            TypedArray attributes = context.obtainStyledAttributes(new int[] {android.R.attr.listDivider});
            mDivider = attributes.getDrawable(0);
            attributes.recycle();
        }

        @Override
        public void onDrawOver(@NonNull Canvas c, RecyclerView parent, @NonNull RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();
            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams)child.getLayoutParams();
                int top = child.getBottom() + params.bottomMargin + (int)child.getTranslationY();
                int bottom = top + mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
        }
    }

    public static class ContextMenuInfo implements ContextMenu.ContextMenuInfo {
        public final View mTargetView;
        public final int mPosition;
        public final long mId;

        public ContextMenuInfo(View targetView, int position, long id) {
            mTargetView = targetView;
            mPosition = position;
            mId = id;
        }
    }

    private final AdapterDataObserver mAdapterObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            updateEmptyView();
        }
    };

    private ContextMenuInfo mContextMenuInfo;
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
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        int position = getChildAdapterPosition(originalView);
        if (position >= 0) {
            long id = getAdapter().getItemId(position);
            mContextMenuInfo = new ContextMenuInfo(originalView, position, id);
            return super.showContextMenuForChild(originalView);
        }
        return false;
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
