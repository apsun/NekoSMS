package com.crossbowffs.nekosms.app;

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.*;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;
import com.crossbowffs.nekosms.widget.RecyclerCursorAdapter;

/* package */ class FilterRulesAdapter extends RecyclerCursorAdapter<FilterRulesAdapter.UserFiltersItemHolder> {
    public static class UserFiltersItemHolder extends RecyclerView.ViewHolder {
        public final TextView mInfoTextView1;
        public final TextView mPatternTextView1;
        public final TextView mInfoTextView2;
        public final TextView mPatternTextView2;
        public SmsFilterData mFilterData;

        public UserFiltersItemHolder(View itemView) {
            super(itemView);
            mInfoTextView1 = (TextView)itemView.findViewById(R.id.filter_rule_info_textview1);
            mPatternTextView1 = (TextView)itemView.findViewById(R.id.filter_rule_pattern_textview1);
            mInfoTextView2 = (TextView)itemView.findViewById(R.id.filter_rule_info_textview2);
            mPatternTextView2 = (TextView)itemView.findViewById(R.id.filter_rule_pattern_textview2);
        }
    }

    private static final String TAG = FilterRulesAdapter.class.getSimpleName();
    private final FilterRulesFragment mFragment;

    public FilterRulesAdapter(FilterRulesFragment fragment) {
        mFragment = fragment;
    }

    @Override
    public UserFiltersItemHolder onCreateViewHolder(ViewGroup group, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(mFragment.getContext());
        View view = layoutInflater.inflate(R.layout.listitem_filter_rules, group, false);
        return new UserFiltersItemHolder(view);
    }

    @Override
    protected int[] onBindColumns(Cursor cursor) {
        return FilterRuleLoader.get().getColumns(cursor);
    }

    @Override
    public void onBindViewHolder(UserFiltersItemHolder holder, Cursor cursor) {
        SmsFilterData filterData = FilterRuleLoader.get().getData(cursor, getColumns(), holder.mFilterData);
        holder.mFilterData = filterData;

        final long id = filterData.getId();
        SmsFilterPatternData senderPattern = filterData.getSenderPattern();
        SmsFilterPatternData bodyPattern = filterData.getBodyPattern();
        SmsFilterPatternData pattern1;
        SmsFilterPatternData pattern2 = null;
        if (!senderPattern.hasData()) {
            pattern1 = bodyPattern;
        } else if (!bodyPattern.hasData()) {
            pattern1 = senderPattern;
        } else {
            pattern1 = senderPattern;
            pattern2 = bodyPattern;
        }

        holder.mInfoTextView1.setText(buildFilterInfoString(R.string.format_filter_info, pattern1));
        holder.mPatternTextView1.setText(pattern1.getPattern());
        if (pattern2 != null) {
            holder.mInfoTextView2.setText(buildFilterInfoString(R.string.format_filter_info, pattern2));
            holder.mPatternTextView2.setText(pattern2.getPattern());
            holder.mInfoTextView2.setVisibility(View.VISIBLE);
            holder.mPatternTextView2.setVisibility(View.VISIBLE);
        } else {
            holder.mInfoTextView2.setText(null);
            holder.mPatternTextView2.setText(null);
            holder.mInfoTextView2.setVisibility(View.GONE);
            holder.mPatternTextView2.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFragment.startFilterEditorActivity(id);
            }
        });
    }

    private String buildFilterInfoString(int lineId, SmsFilterPatternData patternData) {
        String fieldString = mFragment.getString(getFilterFieldStringId(patternData.getField()));
        String modeString = mFragment.getString(getFilterModeStringId(patternData.getMode()));
        String caseSensitiveString = "";
        if (patternData.isCaseSensitive()) {
            caseSensitiveString = mFragment.getString(R.string.filter_info_case_sensitive);
        }
        return mFragment.getString(lineId, fieldString, modeString, caseSensitiveString);
    }

    private int getFilterFieldStringId(SmsFilterField field) {
        switch (field) {
        case SENDER:
            return R.string.filter_info_field_sender;
        case BODY:
            return R.string.filter_info_field_body;
        default:
            return 0;
        }
    }

    private int getFilterModeStringId(SmsFilterMode mode) {
        switch (mode) {
        case REGEX:
            return R.string.filter_info_mode_regex;
        case WILDCARD:
            return R.string.filter_info_mode_wildcard;
        case CONTAINS:
            return R.string.filter_info_mode_contains;
        case PREFIX:
            return R.string.filter_info_mode_prefix;
        case SUFFIX:
            return R.string.filter_info_mode_suffix;
        case EQUALS:
            return R.string.filter_info_mode_equals;
        default:
            return 0;
        }
    }
}
