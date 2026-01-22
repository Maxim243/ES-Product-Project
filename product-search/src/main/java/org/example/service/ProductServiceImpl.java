package org.example.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;
import lombok.RequiredArgsConstructor;
import org.example.config.EsFieldsConfig;
import org.example.dto.ConceptDocDTO;
import org.example.dto.ProductDTO;
import org.example.dto.ProductRequestDTO;
import org.example.dto.ProductResponseDTO;
import org.example.mappers.ProductMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.example.dto.ProductResponseDTO.buildEmptyProductResponseDTO;

@Component
@RequiredArgsConstructor
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

        SearchResponse<ProductDTO> productDTOSearchResponse = searchProducts(productRequestDTO, mainQuery);

        return productMapper.toProductResponseDTO(productDTOSearchResponse);
    }

    private SearchResponse<ProductDTO> searchProducts(ProductRequestDTO productRequestDTO, Query mainQuery) throws IOException {
        return elasticsearchClient.search(s -> s
                        .index(esFieldsConfig.getIndex().getProductIndex())
                        .from(productRequestDTO.from(esFieldsConfig.getRequest().getDefaultQuerySize(), esFieldsConfig.getRequest().getDefaultQueryPage()))
                        .size(productRequestDTO.getValidatedSize(esFieldsConfig.getRequest().getDefaultQuerySize()))
                        .query(mainQuery)
                        .sort(so -> so.score(ss -> ss.order(SortOrder.Desc)))
                        .aggregations(esFieldsConfig.getFields().getBrand(), brand -> brand
                                .terms(term -> term
                                        .field(esFieldsConfig.getFields().getBrandKeyword())
                                        .size(productRequestDTO.getValidatedSize(esFieldsConfig.getRequest().getDefaultQuerySize()))
                                        .order(List.of(
                                                NamedValue.of(esFieldsConfig.getAggregation().getCount(), SortOrder.Desc),
                                                NamedValue.of(esFieldsConfig.getAggregation().getKey(), SortOrder.Asc)
                                        ))))
                        .aggregations(esFieldsConfig.getAggregation().getPriceRanges(), a -> a
                                .range(r -> r
                                        .field(esFieldsConfig.getFields().getPrice())
                                        .ranges(
                                                AggregationRange.of(rb -> rb.to(esFieldsConfig.getAggregation().getCheapPrice()).key(esFieldsConfig.getAggregation().getCheap())),
                                                AggregationRange.of(rb -> rb.from(esFieldsConfig.getAggregation().getCheapPrice()).to(esFieldsConfig.getAggregation().getExpensivePrice())
                                                        .key(esFieldsConfig.getAggregation().getAverage())),
                                                AggregationRange.of(rb -> rb.from(esFieldsConfig.getAggregation().getExpensivePrice()).key(esFieldsConfig.getAggregation().getExpensive()))
                                        )
                                )
                        )
                ,
                ProductDTO.class
        );
    }

    private SearchResponse<ConceptDocDTO> getConceptDocSearchResponse(List<String> textQueryInputTerms) throws IOException {
        Query termsQuery = Query.of(q -> q
                .terms(t -> t
                        .field(esFieldsConfig.getIndex().getSearchTerms())
                        .terms(TermsQueryField.of(f -> f
                                .value(textQueryInputTerms.stream().map(FieldValue::of).toList())
                        ))
                )
        );

        return elasticsearchClient.search(
                s -> s
                        .index(esFieldsConfig.getIndex().getConceptIndex())
                        .query(termsQuery),
                ConceptDocDTO.class
        );
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
        conceptDocDTOList.stream()
                .filter(fieldToTermDTO -> !fieldToTermDTO.type().startsWith(esFieldsConfig.getNested().getSkus()))
                .map(fieldToTermDTO -> Query.of(q -> q
                        .term(t -> t
                                .field(String.format("%s.%s", fieldToTermDTO.type(), esFieldsConfig.getFields().getKeyword()))
                                .value(fieldToTermDTO.originalTerm())
                        )
                ))
                .forEach(filterQueries::add);

        List<Query> nestedSkuQueries = conceptDocDTOList.stream()
                .filter(conceptDocDTO -> conceptDocDTO.type().startsWith(esFieldsConfig.getNested().getSkus()))
                .map(filteredConcept -> Query.of(q -> q
                        .term(t -> t
                                .field(filteredConcept.type())
                                .value(filteredConcept.originalTerm())
                        )
                ))
                .toList();

        if (!nestedSkuQueries.isEmpty()) {
            filterQueries.add(Query.of(q -> q
                    .nested(n -> n
                            .path(esFieldsConfig.getNested().getSkus())
                            .query(q2 -> q2.bool(b -> b.filter(nestedSkuQueries)))
                    )
            ));
        }

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