package org.example.exception;

public class AISearchParsingException extends RuntimeException {
    public AISearchParsingException(String message) {
        super(message);
    }

    public AISearchParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
