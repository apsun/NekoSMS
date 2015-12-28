package com.crossbowffs.nekosms.preferences;

public class BooleanPreference extends BasePreference {
    private final boolean mDefaultValue;

    public BooleanPreference(String key, boolean defaultValue) {
        super(key);
        mDefaultValue = defaultValue;
    }

    public boolean getDefaultValue() {
        return mDefaultValue;
    }
}
