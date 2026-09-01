package org.example.notification.service.exception;

public class EmailNotConfiguredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailNotConfiguredException() {
        super("Mail sender is not configured; skipping email send");
    }
}
