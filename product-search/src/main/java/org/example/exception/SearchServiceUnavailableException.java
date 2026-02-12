package org.example.exception;

public class SearchServiceUnavailableException extends RuntimeException {
    public SearchServiceUnavailableException() {
        super();
    }

    public SearchServiceUnavailableException(String message) {
        super(message);
    }
}
