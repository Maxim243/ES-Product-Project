package org.example.enums;

import lombok.Getter;

@Getter
public enum SearchMessage {
    STRICT_SUCCESS("Here’s what we found for your search"),
    FILTERS_REMOVED("No matches with your filters — here are some suggestions"),
    NO_RESULTS("We couldn’t find any products matching your request");

    private final String message;

    SearchMessage(String message) {
        this.message = message;
    }
}
