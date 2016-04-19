package com.crossbowffs.nekosms.filterlists;

import java.util.List;

public class FilterList {
    /* package */ FilterListInfo mInfo;
    /* package */ List<FilterListRule> mRules;

    public FilterListInfo getInfo() {
        return mInfo;
    }

    public List<FilterListRule> getRules() {
        return mRules;
    }
}
