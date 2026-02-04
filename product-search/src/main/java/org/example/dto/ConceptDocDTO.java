package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record ConceptDocDTO(
        @JsonProperty("search_terms")
        List<String> searchTerms,
        @JsonProperty("original_term")
        String originalTerm,
        String type) {
}
