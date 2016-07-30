package com.crossbowffs.nekosms.backup;

/* package */ class BackupVersionException extends InvalidBackupException {
    public BackupVersionException() {

    }

    public BackupVersionException(String detailMessage) {
        super(detailMessage);
    }

    public BackupVersionException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public BackupVersionException(Throwable throwable) {
        super(throwable);
    }
}
