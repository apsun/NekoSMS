package com.crossbowffs.nekosms.preferences;

public abstract class BasePreference {
    private final String mKey;

    public BasePreference(String key) {
        mKey = key;
    }

    public String getKey() {
        return mKey;
    }
}
