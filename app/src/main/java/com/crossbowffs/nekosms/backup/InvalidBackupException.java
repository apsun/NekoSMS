package com.crossbowffs.nekosms.backup;

/* package */ class InvalidBackupException extends Exception {
    public InvalidBackupException() {

    }

    public InvalidBackupException(String detailMessage) {
        super(detailMessage);
    }

    public InvalidBackupException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public InvalidBackupException(Throwable throwable) {
        super(throwable);
    }
}
