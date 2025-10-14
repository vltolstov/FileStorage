package org.filestorage.app.exception;

public class UserAlreadyExistException extends RuntimeException {
    public UserAlreadyExistException() {
        super("username already exist");
    }
}
