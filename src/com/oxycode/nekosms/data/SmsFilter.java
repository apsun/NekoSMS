package com.oxycode.nekosms.data;

public interface SmsFilter {
    public boolean matches(String sender, String body);
}
