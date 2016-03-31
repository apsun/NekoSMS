package com.crossbowffs.nekosms.preferences;

public class PrefItem<T> {
    private final String mKey;
    private final T mDefaultValue;

    public PrefItem(String key, T defaultValue) {
        mKey = key;
        mDefaultValue = defaultValue;
    }

    public String getKey() {
        return mKey;
    }

    public T getDefaultValue() {
        return mDefaultValue;
    }
}
