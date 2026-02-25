package org.example.enums;

public enum FitStatus {
    FITS,
    NEED_MORE_INFO,
    DOES_NOT_FIT;

    public String buildMessage() {
        return "Autoparts found with status: %s".formatted(this);
    }
}
