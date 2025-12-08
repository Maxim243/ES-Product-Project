package org.example.dto;

import lombok.Builder;

import java.util.Objects;

@Builder
public record ProductRequestDTO(String queryText,
                                Integer size,
                                Integer page) {

    public Integer getValidatedSize(Integer defaultSize) {
        if (Objects.isNull(this.size)) {
            return defaultSize;
        }
        return this.size;
    }

    public Integer getValidatedPage(Integer defaultPage) {
        if (Objects.isNull(this.page)) {
            return defaultPage;
        }
        return this.page;
    }
    public Integer from(Integer defaultSize, Integer defaultPage) {
        return getValidatedSize(defaultSize) * getValidatedPage(defaultPage);
    }
}
