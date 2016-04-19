package com.crossbowffs.nekosms.loader;

import android.content.Context;
import com.crossbowffs.nekosms.data.SmsFilterData;

public abstract class SmsFilterLoader extends AutoContentLoader<SmsFilterData> {
    public SmsFilterLoader(Class<?> contractCls) {
        super(contractCls);
    }

    public abstract CursorWrapper<SmsFilterData> queryAllWhitelistFirst(Context context);
}
