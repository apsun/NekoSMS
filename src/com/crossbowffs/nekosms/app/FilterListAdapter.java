package com.crossbowffs.nekosms.app;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.crossbowffs.nekosms.R;
import com.crossbowffs.nekosms.data.SmsFilterData;
import com.crossbowffs.nekosms.data.SmsFilterField;
import com.crossbowffs.nekosms.data.SmsFilterLoader;
import com.crossbowffs.nekosms.data.SmsFilterMode;
import com.crossbowffs.nekosms.provider.NekoSmsContract;

import java.util.Map;

/* package */ class FilterListAdapter extends RecyclerCursorAdapter<FilterListAdapter.FilterListItemHolder> {
    public class FilterListItemHolder extends RecyclerView.ViewHolder {
        public SmsFilterData mFilterData;
        public TextView mPatternTextView;
        public TextView mFieldTextView;
        public TextView mModeTextView;
        public TextView mCaseSensitiveTextView;

        public FilterListItemHolder(View itemView) {
            super(itemView);

            mPatternTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_pattern_textview);
            mFieldTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_field_textview);
            mModeTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_mode_textview);
            mCaseSensitiveTextView = (TextView)itemView.findViewById(R.id.listitem_filter_list_case_sensitive_textview);
        }
    }

    private int[] mColumns;
    private Map<SmsFilterField, String> mSmsFilterFieldMap;
    private Map<SmsFilterMode, String> mSmsFilterModeMap;
    private String mPatternFormatString;
    private String mFieldFormatString;
    private String mModeFormatString;
    private String mCaseSensitiveFormatString;

    public FilterListAdapter(Context context) {
        mSmsFilterFieldMap = FilterEnumMaps.getFieldMap(context);
        mSmsFilterModeMap = FilterEnumMaps.getModeMap(context);

        Resources resources = context.getResources();
        mPatternFormatString = resources.getString(R.string.format_filter_pattern);
        mFieldFormatString = resources.getString(R.string.format_filter_field);
        mModeFormatString = resources.getString(R.string.format_filter_mode);
        mCaseSensitiveFormatString = resources.getString(R.string.format_filter_case_sensitive);
    }

    @Override
    public FilterListItemHolder onCreateViewHolder(ViewGroup group, int i) {
        LayoutInflater layoutInflater = LayoutInflater.from(group.getContext());
        View view = layoutInflater.inflate(R.layout.listitem_filter_list, group, false);
        return new FilterListItemHolder(view);
    }

    @Override
    public void onBindViewHolder(FilterListItemHolder holder, Cursor cursor) {
        if (mColumns == null) {
            mColumns = SmsFilterLoader.getColumns(cursor);
        }

        SmsFilterData filterData = SmsFilterLoader.getFilterData(cursor, mColumns, holder.mFilterData);
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

        // TODO: Hack
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                Intent intent = new Intent(context, FilterEditorActivity.class);
                Uri filterUri = ContentUris.withAppendedId(NekoSmsContract.Filters.CONTENT_URI, id);
                intent.setData(filterUri);
                context.startActivity(intent);
            }
        });
    }
}
