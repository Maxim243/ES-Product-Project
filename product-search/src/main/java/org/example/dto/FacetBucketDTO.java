package org.example.dto;

import lombok.Builder;

@Builder
public record FacetBucketDTO(String value,
                             Long count) {
}
