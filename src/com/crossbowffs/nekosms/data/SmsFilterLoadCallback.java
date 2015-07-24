package com.crossbowffs.nekosms.data;

public abstract class SmsFilterLoadCallback {
    public void onBegin(int count) { }
    public abstract void onSuccess(SmsFilterData filterData);
    public void onError(InvalidFilterException e) { }
}
