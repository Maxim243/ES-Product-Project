package org.example.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.NamedValue;
import lombok.RequiredArgsConstructor;
import org.example.config.ProductAttributesProperties;
import org.example.dto.ProductDTO;
import org.example.dto.ProductRequestDTO;
import org.example.dto.ProductResponseDTO;
import org.example.mappers.ProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.example.dto.ProductResponseDTO.buildEmptyProductResponseDTO;

@Component
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    @Value("${request.default.defaultQuerySize}")
    private Integer defaultFindByQuerySize;

    @Value("${request.default.defaultQueryPage}")
    private Integer defaultQueryPage;

    @Value("${request.alias}")
    private String alias;

    private final ElasticsearchClient elasticsearchClient;

    private final ProductAttributesProperties productAttributesProperties;

    private final ProductMapper productMapper;

    private final String CHEAP = "Cheap";
    private final String AVERAGE = "Average";
    private final String EXPENSIVE = "Expensive";

    @Override
    public ProductResponseDTO getSearchProductResponse(ProductRequestDTO productRequestDTO) throws IOException {
        if (Objects.isNull(productRequestDTO.queryText())) {
            return buildEmptyProductResponseDTO();
        }
        String textQueryInput = productRequestDTO.queryText().toLowerCase();
        String excludeColorAndSizeQueryString = excludeColorAndSizeFromQuery(textQueryInput);

        List<Query> mainQueriesList = new ArrayList<>();
        mainQueriesList.add(buildBrandAndNameCrossFieldsQuery(excludeColorAndSizeQueryString));
        mainQueriesList.add(buildBrandAndNameBestFieldsQuery(excludeColorAndSizeQueryString));
        mainQueriesList.add(buildSkuQuery(textQueryInput));

        SearchResponse<ProductDTO> searchResponse = searchProducts(productRequestDTO, mainQueriesList);

        return productMapper.toProductResponseDTO(searchResponse);
    }

    private SearchResponse<ProductDTO> searchProducts(ProductRequestDTO productRequestDTO, List<Query> mainQueriesList) throws IOException {
        return elasticsearchClient.search(s -> s
                        .index(alias)
                        .from(productRequestDTO.from(defaultFindByQuerySize, defaultQueryPage))
                        .size(productRequestDTO.getValidatedSize(defaultFindByQuerySize))
                        .query(q -> q
                                .disMax(dm -> dm
                                        .tieBreaker(0.7d)
                                        .queries(mainQueriesList)
                                )
                        )
                        .sort(so -> so.score(ss -> ss.order(SortOrder.Desc)))
                        .aggregations("brand", brand -> brand
                                .terms(term -> term
                                        .field("brand.keyword")
                                        .size(10)
                                        .order(List.of(
                                                NamedValue.of("_count", SortOrder.Desc),
                                                NamedValue.of("_key", SortOrder.Asc)
                                        ))))
                        .aggregations("price_ranges", a -> a
                                .range(r -> r
                                        .field("price")
                                        .ranges(
                                                AggregationRange.of(rb -> rb.to(100.0).key(CHEAP)),
                                                AggregationRange.of(rb -> rb.from(100.0).to(500.0).key(AVERAGE)),
                                                AggregationRange.of(rb -> rb.from(500.0).key(EXPENSIVE))
                                        )
                                )
                        )
                ,
                ProductDTO.class
        );
    }

    private Query buildBrandAndNameCrossFieldsQuery(String queryText) {
        return Query.of(q -> q
                .multiMatch(mm -> mm
                        .query(queryText)
                        .fields("brand", "name")
                        .type(TextQueryType.CrossFields)
                        .operator(Operator.And)
                        .boost(1.0f)
                )
        );
    }

    private Query buildBrandAndNameBestFieldsQuery(String queryText) {
        return Query.of(q -> q
                .multiMatch(mm -> mm
                        .query(queryText)
                        .fields("brand.shingles^5", "name.shingles^7")
                        .operator(Operator.And)
                        .type(TextQueryType.BestFields)
                ));
    }

    private Query buildSkuQuery(String textInputQuery) {
        List<FieldValue> sizeValues = Arrays.stream(textInputQuery.split("\\s+"))
                .filter(productAttributesProperties.getSizes()::contains)
                .map(FieldValue::of)
                .toList();

        List<FieldValue> colorValues = Arrays.stream(textInputQuery.split("\\s+"))
                .filter(productAttributesProperties.getColors()::contains)
                .map(FieldValue::of)
                .toList();

        if (sizeValues.isEmpty() && colorValues.isEmpty()) {
            return Query.of(q -> q.matchNone(mn -> mn));
        }

        return Query.of(q -> q.nested(n -> n
                .path("skus")
                .query(nq -> nq.bool(b -> {
                    List<Query> shouldQueries = new ArrayList<>();
                    if (!sizeValues.isEmpty() && !colorValues.isEmpty()) {
                        shouldQueries.add(Query.of(inner -> inner.bool(bb -> bb
                                .must(Query.of(inner2 -> inner2.terms(t -> t.field("skus.size").terms(v -> v.value(sizeValues)))))
                                .must(Query.of(inner2 -> inner2.terms(t -> t.field("skus.color").terms(v -> v.value(colorValues)))))
                                .boost(3.0f)
                        )));
                    }

                    if (!sizeValues.isEmpty()) {
                        shouldQueries.add(Query.of(inner -> inner.terms(t -> t.field("skus.size").terms(v -> v.value(sizeValues)))));
                    }
                    if (!colorValues.isEmpty()) {
                        shouldQueries.add(Query.of(inner -> inner.terms(t -> t.field("skus.color").terms(v -> v.value(colorValues)))));
                    }
                    b.should(shouldQueries);
                    b.minimumShouldMatch(String.valueOf(1));
                    return b;
                }))
        ));
    }



    private String excludeColorAndSizeFromQuery(String queryText) {
        queryText = queryText.toLowerCase();

        return Arrays.stream(queryText.split("\\s+"))
                .filter(token -> !productAttributesProperties.getColors().contains(token))
                .filter(token -> !productAttributesProperties.getSizes().contains(token))
                .collect(Collectors.joining(" "));
    }
}