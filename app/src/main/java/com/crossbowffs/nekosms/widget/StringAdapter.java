package com.crossbowffs.nekosms.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.List;

public class StringAdapter<T> extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final int mMainLayout;
    private final int mDropdownLayout;
    private final LinkedHashMap<T, String> mItems;
    private final List<T> mItemOrder;

    public StringAdapter(Context context, int layout, LinkedHashMap<T, String> items) {
        this(context, layout, layout, items);
    }

    public StringAdapter(Context context, int mainLayout, int dropdownLayout, LinkedHashMap<T, String> items) {
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMainLayout = mainLayout;
        mDropdownLayout = dropdownLayout;
        mItems = items;
        mItemOrder = List.copyOf(items.keySet());
    }

    public int getPosition(T item) {
        return mItemOrder.indexOf(item);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public T getItem(int position) {
        return mItemOrder.get(position);
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
        return mItems.get(item);
    }
}
