package org.example.dto;

import lombok.Builder;

@Builder
public record AICandidateDoc(String id,
                             String name) {
}
