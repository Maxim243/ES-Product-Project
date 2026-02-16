package org.example.enums;

import lombok.Getter;

@Getter
public enum SearchMessage {
    SEARCH_SUCCESS("Here’s what we found for your search"),
    CATEGORY_ONLY_STRICT_SUCCESS("No matches with your filters — here are some suggestions"),
    NO_RESULTS("We couldn’t find any products matching your request");

    private final String message;

    SearchMessage(String message) {
        this.message = message;
    }
}
