package org.example.pantrypilot.service.exception;

public class StaleChatActionException extends RuntimeException {

    public StaleChatActionException(String message) {
        super(message);
    }
}
