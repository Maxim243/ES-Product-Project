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
import org.elasticsearch.client.RestHighLevelClient;
import org.example.config.ProductAttributesProperties;
import org.example.dto.ProductDTO;
import org.example.dto.ProductRequestDTO;
import org.example.dto.ProductResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    @Override
    public ProductResponseDTO getSearchProductResponse(ProductRequestDTO productRequestDTO) throws IOException {
        if (productRequestDTO.queryText().isEmpty()) {
            return buildEmptyProductResponseDTO();
        }
        String textQueryInput = productRequestDTO.queryText().toLowerCase();
        String excludeColorAndSizeQueryString = excludeColorAndSizeFromQuery(textQueryInput);

        List<Query> mainQueriesList = new ArrayList<>();
        mainQueriesList.add(buildBrandAndNameCrossFieldsQuery(excludeColorAndSizeQueryString));
        mainQueriesList.add(buildBrandAndNameBestFieldsQuery(excludeColorAndSizeQueryString));

        if (!getSizesFromQuery(textQueryInput).isEmpty()) {
            mainQueriesList.add(buildSizeQuery(productAttributesProperties.getSizes()));
        }

        if (!getColorsFromQuery(textQueryInput).isEmpty()) {
            mainQueriesList.add(buildColorQuery(productAttributesProperties.getColors()));
        }

        searchProducts(productRequestDTO, mainQueriesList);


        return null;
    }

    private void searchProducts(ProductRequestDTO productRequestDTO, List<Query> mainQueriesList) throws IOException {
        SearchResponse<ProductDTO> productDTOSearchResponse = elasticsearchClient.search(s -> s
                        .index(alias)
                        .from(productRequestDTO.from(defaultFindByQuerySize, defaultQueryPage))
                        .size(defaultFindByQuerySize)
                        .query(q -> q
                                .disMax(dm -> dm
                                        .tieBreaker(0.7d)
                                        .queries(mainQueriesList)
                                )
                        )
                        .sort(so -> so.score(ss -> ss.order(SortOrder.Desc)))   // _score DESC
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
                                                AggregationRange.of(rb -> rb.to(100.0).key("Cheap")),
                                                AggregationRange.of(rb -> rb.from(100.0).to(500.0).key("Average")),
                                                AggregationRange.of(rb -> rb.from(500.0).key("Expensive"))
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
                        .fields("brand.text", "name.text")
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
                        .fields("brand.shingles^5", "name.shingles^5")
                        .type(TextQueryType.BestFields)
                ));
    }

    private Query buildSizeQuery(List<String> sizes) {
        List<FieldValue> sizeValues = sizes.stream()
                .map(FieldValue::of)
                .toList();

        return Query.of(q -> q
                .functionScore(fs -> fs
                        .query(t -> t
                                .terms(term -> term
                                        .field("skus.size")
                                        .terms(v -> v.value(sizeValues))
                                )
                        )
                        .functions(f -> f.weight(2.0))
                )
        );
    }

    private Query buildColorQuery(List<String> colors) {
        List<FieldValue> colorValues = colors.stream()
                .map(FieldValue::of)
                .toList();

        return Query.of(q -> q
                .functionScore(fs -> fs
                        .query(t -> t
                                .terms(term -> term
                                        .field("skus.color")
                                        .terms(v -> v.value(colorValues))
                                )
                        )
                        .functions(f -> f.weight(2.0))
                )
        );
    }

    private String excludeColorAndSizeFromQuery(String queryText) {
        queryText = queryText.toLowerCase();

        return Arrays.stream(queryText.split("\\s+"))
                .filter(token -> !productAttributesProperties.getColors().contains(token))
                .filter(token -> !productAttributesProperties.getSizes().contains(token))
                .collect(Collectors.joining(" "));
    }

    private List<String> getColorsFromQuery(String queryText) {
        return Arrays.stream(queryText.split("\\s+"))
                .filter(token -> productAttributesProperties.getColors().contains(token))
                .toList();
    }

    private List<String> getSizesFromQuery(String queryText) {
        return Arrays.stream(queryText.split("\\s+"))
                .filter(token -> productAttributesProperties.getSizes().contains(token))
                .toList();
    }
}