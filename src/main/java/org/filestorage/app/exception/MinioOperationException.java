package org.filestorage.app.exception;

public class MinioOperationException extends RuntimeException {
    public MinioOperationException(String message) {
        super(message);
    }
}
