package org.example.dto;

import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Builder
public record ProductResponseDTO(Long totalHits,
                                 List<ProductDTO> productDTOList,
                                 FacetDTO facetDTO) {

    public static ProductResponseDTO buildEmptyProductResponseDTO() {
        return ProductResponseDTO.builder()
                .totalHits(0L)
                .productDTOList(Collections.emptyList())
                .facetDTO(FacetDTO
                        .builder()
                        .facetBucketDTO(Collections.emptyMap())
                        .build())
                .build();
    }
}
