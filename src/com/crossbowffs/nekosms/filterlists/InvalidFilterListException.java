package com.crossbowffs.nekosms.filterlists;

public class InvalidFilterListException extends RuntimeException {
    private int mLineNumber;

    public InvalidFilterListException(int lineNumber, String detailMessage) {
        super(detailMessage);
        mLineNumber = lineNumber;
    }

    public InvalidFilterListException(int lineNumber, String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
        mLineNumber = lineNumber;
    }

    public int getLineNumber() {
        return mLineNumber;
    }
}
