package com.crossbowffs.nekosms.widget;

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
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final int mMainLayout;
    private final int mDropdownLayout;
    private final List<T> mItems;
    private Map<T, String> mStringMap;

    public EnumAdapter(Context context, int layout, Class<T> cls) {
        this(context, layout, layout, cls);
    }

    public EnumAdapter(Context context, int layout, T[] items) {
        this(context, layout, layout, items);
    }

    public EnumAdapter(Context context, int mainLayout, int dropdownLayout, Class<T> cls) {
        this(context, mainLayout, dropdownLayout, cls.getEnumConstants());
    }

    public EnumAdapter(Context context, int mainLayout, int dropdownLayout, T[] items) {
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMainLayout = mainLayout;
        mDropdownLayout = dropdownLayout;
        mItems = Arrays.asList(items);
    }

    public void setStringMap(Map<T, String> map) {
        mStringMap = map;
    }

    public Map<T, String> getStringMap() {
        return mStringMap;
    }

    public Context getContext() {
        return mContext;
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
            convertView = mInflater.inflate(mMainLayout, parent, false);
        }
        bindMainView(convertView, getItem(position));
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(mDropdownLayout, parent, false);
        }
        bindDropdownView(convertView, getItem(position));
        return convertView;
    }

    private void defaultBindView(View view, T item) {
        if (view instanceof TextView) {
            TextView textView = (TextView)view;
            String itemString = getItemString(item);
            textView.setText(itemString);
            return;
        }

        throw new IllegalStateException("Cannot automatically bind view of type " + view.getClass().getName());
    }

    protected void bindMainView(View view, T item) {
        defaultBindView(view, item);
    }

    protected void bindDropdownView(View view, T item) {
        defaultBindView(view, item);
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
