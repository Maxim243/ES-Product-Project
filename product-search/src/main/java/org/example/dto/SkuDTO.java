package org.example.dto;

import lombok.Builder;

@Builder
public record SkuDTO(String color,
                     String size) {
}
