package org.example.utils;

import lombok.experimental.UtilityClass;
import org.example.dto.AICandidateDoc;

import java.util.List;

@UtilityClass
public class AIPromptUtil {

    public String buildAIPrompt(
            String userQuery,
            List<AICandidateDoc> docs
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                You are a product search ranking system.
                Your task is to select the most relevant product IDs
                based strictly on the user query.
                
                Rules:
                - Consider product names only
                - Do NOT invent products
                - Return only IDs that best match the query
                - Order IDs from best to worst
                - If none are relevant, return an empty list
                - Limit 20 products or less
                - Return in JSON format
                
                User query:
                """);

        sb.append(userQuery).append("\n\n");

        sb.append("Candidate products:\n");

        docs.forEach(doc ->
                sb.append("- ID: ")
                        .append(doc.id())
                        .append(", Name: ")
                        .append(doc.name())
                        .append("\n")
        );

        sb.append("""
                Output format:
                ["id1","id2","id3"]
                """);

        return sb.toString();
    }

}
