package org.example.dto;

import lombok.Builder;
import lombok.Data;
import org.example.enums.SearchMessage;

import java.util.Collections;
import java.util.List;

@Data
@Builder
public class ProductResponseDTO {
    private Long totalHits;
    private String message;
    private List<ProductDTO> productDTOList;
    private FacetDTO facetDTO;

    public static ProductResponseDTO buildEmptyProductResponseDTO() {
        return ProductResponseDTO.builder()
                .totalHits(0L)
                .productDTOList(Collections.emptyList())
                .facetDTO(FacetDTO
                        .builder()
                        .facetBucketDTO(Collections.emptyMap())
                        .build())
                .message(SearchMessage.NO_RESULTS.getMessage())
                .build();
    }
}
