package com.crossbowffs.nekosms.app;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.database.SmsFilterDbLoader;
import com.crossbowffs.nekosms.provider.NekoSmsContract;
import com.crossbowffs.nekosms.utils.Xlog;

import java.util.Map;

/* package */ class FilterListAdapter extends RecyclerCursorAdapter<FilterListAdapter.FilterListItemHolder> {
    public static class FilterListItemHolder extends RecyclerView.ViewHolder {
        public final TextView mPatternTextView;
        public final TextView mFieldTextView;
        public final TextView mModeTextView;
        public final TextView mCaseSensitiveTextView;
        public SmsFilterData mFilterData;

        public FilterListItemHolder(View itemView) {
            super(itemView);

            mPatternTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_pattern_textview);
            mFieldTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_field_textview);
            mModeTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_mode_textview);
            mCaseSensitiveTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_case_sensitive_textview);
        }
    }

    private static final String TAG = FilterListAdapter.class.getSimpleName();

    private final FilterListActivity mActivity;
    private final CoordinatorLayout mCoordinatorLayout;
    private final Map<SmsFilterField, String> mSmsFilterFieldMap;
    private final Map<SmsFilterMode, String> mSmsFilterModeMap;
    private final String mPatternFormatString;
    private final String mFieldFormatString;
    private final String mModeFormatString;
    private final String mCaseSensitiveFormatString;
    private int[] mColumns;

    public FilterListAdapter(FilterListActivity activity) {
        mActivity = activity;
        mCoordinatorLayout = (CoordinatorLayout)activity.findViewById(R.id.activity_filter_list_root);
        mSmsFilterFieldMap = FilterEnumMaps.getFieldMap(activity);
        mSmsFilterModeMap = FilterEnumMaps.getModeMap(activity);
        mPatternFormatString = activity.getString(R.string.format_filter_pattern);
        mFieldFormatString = activity.getString(R.string.format_filter_field);
        mModeFormatString = activity.getString(R.string.format_filter_mode);
        mCaseSensitiveFormatString = activity.getString(R.string.format_filter_case_sensitive);
    }

    @Override
    public FilterListItemHolder onCreateViewHolder(ViewGroup group, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(mActivity);
        View view = layoutInflater.inflate(R.layout.listitem_filter_list, group, false);
        return new FilterListItemHolder(view);
    }

    @Override
    public void onBindViewHolder(FilterListItemHolder holder, Cursor cursor) {
        if (mColumns == null) {
            mColumns = SmsFilterDbLoader.getColumns(cursor);
        }

        SmsFilterData filterData = SmsFilterDbLoader.getFilterData(cursor, mColumns, holder.mFilterData);
        holder.mFilterData = filterData;

        final long id = filterData.getId();
        SmsFilterField field = filterData.getField();
        SmsFilterMode mode = filterData.getMode();
        String pattern = filterData.getPattern();
        boolean caseSensitive = filterData.isCaseSensitive();

        holder.mPatternTextView.setText(String.format(mPatternFormatString, pattern));
        holder.mFieldTextView.setText(String.format(mFieldFormatString, mSmsFilterFieldMap.get(field)));
        holder.mModeTextView.setText(String.format(mModeFormatString, mSmsFilterModeMap.get(mode)));
        holder.mCaseSensitiveTextView.setText(String.format(mCaseSensitiveFormatString, caseSensitive));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mActivity, FilterEditorActivity.class);
                Uri filterUri = ContentUris.withAppendedId(NekoSmsContract.Filters.CONTENT_URI, id);
                intent.setData(filterUri);
                mActivity.startActivity(intent);
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                deleteFilter(id);
                return true;
            }
        });
    }

    private void deleteFilter(long filterId) {
        final SmsFilterData filterData = SmsFilterDbLoader.loadAndDeleteFilter(mActivity, filterId);
        if (filterData == null) {
            Xlog.e(TAG, "Failed to delete filter: could not load data");
            return;
        }

        Snackbar.make(mCoordinatorLayout, R.string.filter_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SmsFilterDbLoader.writeFilter(mActivity, filterData);
                }
            })
            .show();
    }
}
