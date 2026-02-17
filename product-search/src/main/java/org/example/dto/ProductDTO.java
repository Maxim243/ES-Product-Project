package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record ProductDTO(String brand,
                         String name,
                         Double price,
                         String category,
                         @JsonProperty("skus")
                         List<SkuDTO> skuDTOList) {
}
