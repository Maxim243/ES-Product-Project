package org.example.dto;

import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Builder
public record AutoPartResponseDTO(String message,
                                  Integer count,
                                  List<AutoPartFitDTO> autoPartFitDTOS) {

    public static AutoPartResponseDTO buildEmptyResponse() {
        return AutoPartResponseDTO
                .builder()
                .message("We could not find anything with your filters")
                .count(0)
                .autoPartFitDTOS(Collections.emptyList())
                .build();
    }
}
