package org.example.pantrypilot.service.exception;

public class EmailAlreadyTakenException extends RuntimeException {

    public EmailAlreadyTakenException() {
        super("Email is already registered");
    }
}
