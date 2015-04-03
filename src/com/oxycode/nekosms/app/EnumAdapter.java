package com.oxycode.nekosms.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EnumAdapter<T extends Enum<T>> extends BaseAdapter {
    public interface ViewBinder<U> {
        boolean bindView(Context context, View view, U item);
    }

    private Context mContext;
    private LayoutInflater mInflater;
    private int mLayout;
    private List<T> mItems;
    private ViewBinder<T> mBinder;
    private Map<T, String> mStringMap;

    public EnumAdapter(Context context, int layout, Class<T> cls) {
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayout = layout;
        mItems = Arrays.asList(cls.getEnumConstants());
    }

    public void setStringMap(Map<T, String> map) {
        mStringMap = map;
    }

    public Map<T, String> getStringMap() {
        return mStringMap;
    }

    public void setViewBinder(ViewBinder<T> binder) {
        mBinder = binder;
    }

    public ViewBinder<T> getViewBinder() {
        return mBinder;
    }

    public int getPosition(T item) {
        return mItems.indexOf(item);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public T getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = newView(mContext, parent);
        }

        bindView(mContext, convertView, getItem(position));
        return convertView;
    }

    protected View newView(Context context, ViewGroup parent) {
        return mInflater.inflate(mLayout, parent, false);
    }

    protected void bindView(Context context, View view, T item) {
        if (mBinder != null && mBinder.bindView(context, view, item)) {
            return;
        }

        if (view instanceof TextView) {
            TextView textView = (TextView)view;
            String itemString = getItemString(item);
            textView.setText(itemString);
            return;
        }

        throw new IllegalStateException("Cannot automatically bind view of type " + view.getClass().getName());
    }

    protected String getItemString(T item) {
        if (mStringMap != null) {
            String itemString = mStringMap.get(item);
            if (itemString != null) {
                return itemString;
            }
        }

        return item.toString();
    }
}
