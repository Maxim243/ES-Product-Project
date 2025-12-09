package org.example.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record FacetDTO(Map<String, List<FacetBucketDTO>> facetBucketDTO) {
}
