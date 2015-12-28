package com.crossbowffs.nekosms.data;

public class InvalidFilterException extends RuntimeException {
    public InvalidFilterException() {
        super();
    }

    public InvalidFilterException(String detailMessage) {
        super(detailMessage);
    }

    public InvalidFilterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidFilterException(Throwable cause) {
        super(cause);
    }
}
