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

/* package */ class FilterRulesAdapter extends RecyclerCursorAdapter<FilterRulesAdapter.UserFiltersItemHolder> {
    public static class UserFiltersItemHolder extends RecyclerView.ViewHolder {
        public final TextView mConfigTextView;
        public final TextView mPatternTextView;
        public SmsFilterData mFilterData;

        public UserFiltersItemHolder(View itemView) {
            super(itemView);

            mConfigTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_config_textview);
            mPatternTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_pattern_textview);
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
        SmsFilterAction action = filterData.getAction();
        /*SmsFilterPatternData senderPattern = filterData.getSenderPattern();
        SmsFilterPatternData bodyPattern = filterData.getBodyPattern();
        SmsFilterField field = filterData.getField();
        SmsFilterMode mode = filterData.getMode();*/
        String pattern = ""; //filterData.getPattern();
        boolean caseSensitive = false; //filterData.isCaseSensitive();

        String actionString = mFragment.getString(getFilterActionStringId(action));
        String fieldString = ""; //mFragment.getString(getFilterFieldStringId(field));
        String modeString = ""; //mFragment.getString(getFilterModeStringId(mode));
        String caseSensitiveString = "";
        if (caseSensitive) {
            caseSensitiveString = mFragment.getString(R.string.filter_config_case_sensitive);
        }
        String configString = mFragment.getString(R.string.format_filter_config, actionString, fieldString, modeString, caseSensitiveString);
        holder.mConfigTextView.setText(configString);
        holder.mPatternTextView.setText(pattern);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFragment.startFilterEditorActivity(id);
            }
        });
    }

    private int getFilterActionStringId(SmsFilterAction action) {
        switch (action) {
        case ALLOW:
            return R.string.filter_config_action_allow;
        case BLOCK:
            return R.string.filter_config_action_block;
        default:
            return 0;
        }
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
        case WILDCARD:
            return R.string.filter_config_mode_wildcard;
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
