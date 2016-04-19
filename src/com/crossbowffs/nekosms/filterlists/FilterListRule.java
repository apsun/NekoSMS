package com.crossbowffs.nekosms.filterlists;

public class FilterListRule {
    /* package */ String mSenderPattern;
    /* package */ String mBodyPattern;
    /* package */ boolean mIsWhitelist;

    public String getSenderPattern() {
        return mSenderPattern;
    }

    public String getBodyPattern() {
        return mBodyPattern;
    }

    public boolean isWhitelist() {
        return mIsWhitelist;
    }
}
