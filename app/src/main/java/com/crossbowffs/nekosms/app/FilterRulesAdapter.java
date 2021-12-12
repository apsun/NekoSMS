package com.crossbowffs.nekosms.app;

import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterAction;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.data.SmsFilterPatternData;
import com.crossbowffs.nekosms.loader.FilterRuleLoader;
import com.crossbowffs.nekosms.widget.RecyclerCursorAdapter;

import java.util.List;

/* package */ public class FilterRulesAdapter extends RecyclerCursorAdapter<FilterRulesAdapter.UserFiltersItemHolder> {

    public static class UserFiltersItemHolder extends RecyclerView.ViewHolder {
        public final TextView mActionInfoTextView;
        public final TextView mSenderInfoTextView;
        public final TextView mSenderPatternTextView;
        public final TextView mBodyInfoTextView;
        public final TextView mBodyPatternTextView;
        public SmsFilterData mFilterData;

        public UserFiltersItemHolder(View itemView) {
            super(itemView);
            mActionInfoTextView = (TextView)itemView.findViewById(R.id.filter_rule_action_info_textview);
            mSenderInfoTextView = (TextView)itemView.findViewById(R.id.filter_rule_sender_info_textview);
            mSenderPatternTextView = (TextView)itemView.findViewById(R.id.filter_rule_sender_pattern_textview);
            mBodyInfoTextView = (TextView)itemView.findViewById(R.id.filter_rule_body_info_textview);
            mBodyPatternTextView = (TextView)itemView.findViewById(R.id.filter_rule_body_pattern_textview);
        }
    }

    private final FilterRulesFragment mFragment;
    private List<SmsFilterData> mSmsFilterDatas;

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

        SmsFilterAction action=filterData.getAction();
        SmsFilterPatternData senderPattern = filterData.getSenderPattern();
        SmsFilterPatternData bodyPattern = filterData.getBodyPattern();

        bindTextViews(action, holder.mActionInfoTextView);
        bindTextViews(senderPattern, holder.mSenderInfoTextView, holder.mSenderPatternTextView);
        bindTextViews(bodyPattern, holder.mBodyInfoTextView, holder.mBodyPatternTextView);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFragment.startFilterEditorActivity(id);
            }
        });
    }

    private void bindTextViews(SmsFilterAction action, TextView infoView) {
        infoView.setText(action.name());
        infoView.setVisibility(View.VISIBLE);
        if(action==SmsFilterAction.ALLOW){
            infoView.setTextColor(Color.GREEN);
            infoView.setAlpha(0.5f);
        }else {
            infoView.setTextColor(Color.RED);
            infoView.setAlpha(0.5f);
        }
    }

    private void bindTextViews(SmsFilterPatternData pattern, TextView infoView, TextView patternView) {
        if (pattern.hasData()) {
            infoView.setText(buildFilterInfoString(R.string.format_filter_info, pattern));
            patternView.setText(pattern.getPattern());
            infoView.setVisibility(View.VISIBLE);
            patternView.setVisibility(View.VISIBLE);
        } else {
            infoView.setText("");
            patternView.setText("");
            infoView.setVisibility(View.GONE);
            patternView.setVisibility(View.GONE);
        }
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
