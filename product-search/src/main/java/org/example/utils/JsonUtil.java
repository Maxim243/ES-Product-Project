package org.example.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.example.exception.AISearchParsingException;

import java.util.List;

@UtilityClass
public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<String> parseIds(String raw) {
        try {
            String cleaned = extractJsonArray(raw);

            return OBJECT_MAPPER.readValue(
                    cleaned,
                    new TypeReference<>() {}
            );

        } catch (JsonProcessingException e) {
            throw new AISearchParsingException("Failed to parse AI response", e);
        }
    }

    private static String extractJsonArray(String text) {
        if (text == null) {
            throw new AISearchParsingException("AI response is null");
        }

        int start = text.indexOf("[");
        int end = text.lastIndexOf("]");

        if (start == -1 || end == -1 || end < start) {
            throw new AISearchParsingException(
                    "AI response does not contain valid JSON array: " + text
            );
        }

        return text.substring(start, end + 1);
    }
}
