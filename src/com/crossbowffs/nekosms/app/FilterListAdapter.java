package com.crossbowffs.nekosms.app;

import android.database.Cursor;
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

/* package */ class FilterListAdapter extends RecyclerCursorAdapter<FilterListAdapter.FilterListItemHolder> {
    public static class FilterListItemHolder extends RecyclerView.ViewHolder {
        public final TextView mConfigTextView;
        public final TextView mPatternTextView;
        public SmsFilterData mFilterData;

        public FilterListItemHolder(View itemView) {
            super(itemView);

            mConfigTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_config_textview);
            mPatternTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_pattern_textview);
        }
    }

    private static final String TAG = FilterListAdapter.class.getSimpleName();
    private final FilterListActivity mActivity;
    private int[] mColumns;

    public FilterListAdapter(FilterListActivity activity) {
        mActivity = activity;
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

        String fieldString = mActivity.getString(getFilterFieldStringId(field));
        String modeString = mActivity.getString(getFilterModeStringId(mode));
        String caseSensitiveString = "";
        if (caseSensitive) {
            caseSensitiveString = mActivity.getString(R.string.filter_config_case_sensitive);
        }
        String configString = mActivity.getString(R.string.format_filter_config, fieldString, modeString, caseSensitiveString);
        holder.mConfigTextView.setText(configString);
        holder.mPatternTextView.setText(pattern);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.startFilterEditorActivity(id);
            }
        });
    }

    private int getFilterFieldStringId(SmsFilterField field) {
        switch (field) {
        case SENDER:
            return R.string.filter_config_field_sender;
        case BODY:
            return R.string.filter_config_field_body;
        default:
            return 0;
        }
    }

    private int getFilterModeStringId(SmsFilterMode mode) {
        switch (mode) {
        case REGEX:
            return R.string.filter_config_mode_regex;
        case CONTAINS:
            return R.string.filter_config_mode_contains;
        case PREFIX:
            return R.string.filter_config_mode_prefix;
        case SUFFIX:
            return R.string.filter_config_mode_suffix;
        case EQUALS:
            return R.string.filter_config_mode_equals;
        default:
            return 0;
        }
    }
}
