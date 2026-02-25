package org.example.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.EsFieldsConfig;
import org.example.dto.*;
import org.example.exception.SearchServiceUnavailableException;
import org.example.mappers.AutoPartMapper;
import org.example.utils.AutoPartQueryUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.example.dto.AutoPartResponseDTO.buildEmptyResponse;
import static org.example.enums.FitStatus.*;
import static org.example.utils.AutoPartQueryUtil.addAggregationWithFilters;

@Service
@Slf4j
@RequiredArgsConstructor
public class AutoPartServiceImpl {

    private final EsFieldsConfig esFieldsConfig;

    private final ElasticsearchClient elasticsearchClient;

    private final AutoPartMapper autoPartMapper;

    public AutoPartResponseDTO getAutoParts(AutoPartRequestDTO requestDTO) {
        boolean containsSku = requestDTO.questionAnswerDTOS()
                .stream()
                .anyMatch(q -> "sku".equals(q.question()));

        if (containsSku) {
            return handleSearchWithSku(requestDTO);
        }

        return handleSearchWithoutSku(requestDTO);
    }

    private SearchResponse<AutoPartDocDTO> searchAutoParts(AutoPartRequestDTO autoPartRequestDTO,
                                                           Query mainQuery,
                                                           boolean addAggregation) {

        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(esFieldsConfig.getIndex().getAutoPartIndex())
                .query(mainQuery)
                .sort(so -> so.score(ss -> ss.order(SortOrder.Desc)));

        if (addAggregation) {
            addAggregationWithFilters(searchBuilder, autoPartRequestDTO.questionAnswerDTOS(), esFieldsConfig);
        }

        try {
            return elasticsearchClient.search(searchBuilder.build(), AutoPartDocDTO.class);
        } catch (IOException e) {
            log.error("Search stage failed", e);
            throw new SearchServiceUnavailableException(e.getMessage());
        }
    }

    private AutoPartResponseDTO handleSearchWithoutSku(AutoPartRequestDTO autoPartRequestDTO) {
        Query mainQueryWithoutSku = AutoPartQueryUtil.createMainQueryWithoutSku(autoPartRequestDTO.questionAnswerDTOS());
        SearchResponse<AutoPartDocDTO> searchResponse = searchAutoParts(autoPartRequestDTO, mainQueryWithoutSku, false);

        List<AutoPartFitDTO> autoPartFitDTOS = searchResponse
                .hits()
                .hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(autoPartDocDTO -> autoPartMapper
                        .mapAutoPartDocToAutoPartFitDTO(autoPartDocDTO, NEED_MORE_INFO))
                .toList();

        if (autoPartFitDTOS.isEmpty()) {
            return buildEmptyResponse();
        }

        return AutoPartResponseDTO.builder()
                .message(NEED_MORE_INFO.buildMessage())
                .count(autoPartFitDTOS.size())
                .autoPartFitDTOS(autoPartFitDTOS)
                .build();
    }

    private AutoPartResponseDTO handleSearchWithSku(AutoPartRequestDTO autoPartRequestDTO) {
        Query mainQueryWithSku = AutoPartQueryUtil.createMainQueryWithSku(autoPartRequestDTO.questionAnswerDTOS());
        SearchResponse<AutoPartDocDTO> searchResponse = searchAutoParts(autoPartRequestDTO, mainQueryWithSku, true);

        Aggregate strictMatchesAgg = searchResponse.aggregations().get("strict_matches");
        long docCount = strictMatchesAgg.filter().docCount();

        if (docCount == 0) {
            return getAutoPartsBySku(searchResponse);
        }

        return getAutoPartBySkuAndFilters(strictMatchesAgg);
    }

    private AutoPartResponseDTO getAutoPartBySkuAndFilters(Aggregate strictMatchesAgg) {
        List<AutoPartFitDTO> fitDTOs = strictMatchesAgg
                .filter()
                .aggregations()
                .get("fit_docs")
                .topHits()
                .hits()
                .hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(dto -> {
                    AutoPartFitDTO fitDTO = autoPartMapper.mapJsonToAutoPartFitDTO(dto);
                    fitDTO.setFitStatus(FITS);
                    return fitDTO;
                })
                .toList();

        return AutoPartResponseDTO.builder()
                .message(FITS.buildMessage())
                .count(fitDTOs.size())
                .autoPartFitDTOS(fitDTOs)
                .build();
    }

    private AutoPartResponseDTO getAutoPartsBySku(SearchResponse<AutoPartDocDTO> searchResponse) {
        List<AutoPartFitDTO> autoPartFitDTOS = searchResponse
                .hits()
                .hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(autoPartDocDTO -> autoPartMapper
                        .mapAutoPartDocToAutoPartFitDTO(autoPartDocDTO, DOES_NOT_FIT))
                .toList();

        return AutoPartResponseDTO.builder()
                .message(DOES_NOT_FIT.buildMessage())
                .count(autoPartFitDTOS.size())
                .autoPartFitDTOS(autoPartFitDTOS)
                .build();
    }

}
