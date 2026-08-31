package org.example.notification.service.exception;

public class EmailSendFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailSendFailedException(String message) {
        super(message);
    }

    public EmailSendFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
