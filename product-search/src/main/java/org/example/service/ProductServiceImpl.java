package org.example.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.EsFieldsConfig;
import org.example.dto.ConceptDocDTO;
import org.example.dto.ProductDTO;
import org.example.dto.ProductRequestDTO;
import org.example.dto.ProductResponseDTO;
import org.example.mappers.ProductMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.example.dto.ProductResponseDTO.buildEmptyProductResponseDTO;
import static org.example.utils.QueryUtil.buildMainFilters;
import static org.example.utils.QueryUtil.buildNestedFilters;
import static org.example.utils.QueryUtil.addBrandAggregation;
import static org.example.utils.QueryUtil.addPriceRangeAggregation;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ElasticsearchClient elasticsearchClient;

    private final EsFieldsConfig esFieldsConfig;

    private final ProductMapper productMapper;

    @Override
    public ProductResponseDTO getSearchProductResponse(ProductRequestDTO productRequestDTO) throws IOException {
        if (Objects.isNull(productRequestDTO.queryText())) {
            return buildEmptyProductResponseDTO();
        }
        List<Query> filterQueries = new ArrayList<>();
        List<Query> mustQueries = new ArrayList<>();
        List<Query> shouldQuery = new ArrayList<>();

        List<String> textQueryInputTerms = List.of(productRequestDTO.queryText().toLowerCase().split(" "));

        SearchResponse<ConceptDocDTO> conceptDocSearchResponse = getConceptDocSearchResponse(textQueryInputTerms);

        List<ConceptDocDTO> conceptDocDTOList = conceptDocSearchResponse.hits()
                .hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .toList();

        String productNameFieldTokens = extractProductNameFieldTokens(textQueryInputTerms, conceptDocDTOList);

        Query mainQuery = buildMainQuery(
                addFiltersToQuery(conceptDocDTOList, filterQueries),
                addMustQueries(productNameFieldTokens, mustQueries, esFieldsConfig.getFields().getName()),
                addShouldQueries(productNameFieldTokens, shouldQuery, esFieldsConfig.getFields().getNameShingles())
        );
        log.info("mainQuery: {}", mainQuery);


        SearchResponse<ProductDTO> productDTOSearchResponse = searchProducts(productRequestDTO, mainQuery);

        return productMapper.toProductResponseDTO(productDTOSearchResponse);
    }

    private SearchResponse<ProductDTO> searchProducts(ProductRequestDTO productRequestDTO, Query mainQuery) throws IOException {
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(esFieldsConfig.getIndex().getProductIndex())
                .from(productRequestDTO.from(esFieldsConfig.getRequest().getDefaultQuerySize(), esFieldsConfig.getRequest().getDefaultQueryPage()))
                .size(productRequestDTO.getValidatedSize(esFieldsConfig.getRequest().getDefaultQuerySize()))
                .query(mainQuery)
                .sort(so -> so.score(ss -> ss.order(SortOrder.Desc)));

        addBrandAggregation(searchBuilder, productRequestDTO, esFieldsConfig);
        addPriceRangeAggregation(searchBuilder, esFieldsConfig);

        return elasticsearchClient.search(searchBuilder.build(), ProductDTO.class);
    }

    private SearchResponse<ConceptDocDTO> getConceptDocSearchResponse(List<String> textQueryInputTerms) throws IOException {
        Query conceptTermsQuery = Query.of(q -> q
                .terms(t -> t
                        .field(esFieldsConfig.getIndex().getSearchTerms())
                        .terms(TermsQueryField.of(f -> f
                                .value(textQueryInputTerms.stream().map(FieldValue::of).toList())
                        ))
                )
        );

        log.info("conceptTermsQuery: {}", conceptTermsQuery);

        SearchResponse<ConceptDocDTO> conceptSearchResponse = elasticsearchClient.search(
                s -> s
                        .index(esFieldsConfig.getIndex().getConceptIndex())
                        .query(conceptTermsQuery),
                ConceptDocDTO.class
        );

        log.info("conceptSearchResponse: {}", conceptSearchResponse);

        return conceptSearchResponse;
    }

    private Query buildMainQuery(List<Query> filterQueries, List<Query> mustQueries, List<Query> shouldQueries) {
        return Query.of(q -> q.bool(b -> {
            if (!filterQueries.isEmpty()) {
                b.filter(filterQueries);
            }
            if (!mustQueries.isEmpty()) {
                b.must(mustQueries);
            }
            if (!shouldQueries.isEmpty()) {
                b.should(shouldQueries);
            }
            return b;
        }));
    }

    private List<Query> addMustQueries(String productNameFieldTokens, List<Query> mustQueries, String fieldName) {
        if (!productNameFieldTokens.isBlank()) {
            mustQueries.add(Query.of(q -> q
                    .match(m -> m
                            .field(fieldName)
                            .query(productNameFieldTokens)
                            .boost(2.0f)
                    )
            ));
        }
        return mustQueries;
    }

    private List<Query> addShouldQueries(String productNameFieldTokens, List<Query> shouldQueries, String fieldName) {
        if (!productNameFieldTokens.isBlank()) {
            shouldQueries.add(Query.of(q -> q
                    .matchPhrase(mp -> mp
                            .field(fieldName)
                            .query(productNameFieldTokens)
                            .boost(5.0f)
                    )
            ));
        }
        return shouldQueries;
    }

    private List<Query> addFiltersToQuery(List<ConceptDocDTO> conceptDocDTOList, List<Query> filterQueries) {
        buildMainFilters(conceptDocDTOList, filterQueries, esFieldsConfig);
        buildNestedFilters(conceptDocDTOList, filterQueries, esFieldsConfig);

        return filterQueries;
    }

    private String extractProductNameFieldTokens(List<String> textQueryInputTerms, List<ConceptDocDTO> conceptDocDTOList) {
        return textQueryInputTerms.stream()
                .filter(textInputToken ->
                        conceptDocDTOList.stream()
                                .filter(Objects::nonNull)
                                .noneMatch(conceptDocDTO ->
                                        conceptDocDTO.searchTerms().contains(textInputToken)))
                .collect(Collectors.joining(" "));
    }
}