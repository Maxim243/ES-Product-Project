package org.example.utils;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.util.NamedValue;
import lombok.experimental.UtilityClass;
import org.example.config.EsFieldsConfig;
import org.example.dto.ConceptDocDTO;
import org.example.dto.ProductRequestDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class QueryUtil {

    public static void buildMainFilters(List<ConceptDocDTO> conceptDocDTOList,
                                        List<Query> filterQueries,
                                        EsFieldsConfig esFieldsConfig) {

        Map<String, List<ConceptDocDTO>> rootFilters =
                conceptDocDTOList.stream()
                        .filter(conceptDocDTO ->
                                !conceptDocDTO.type().startsWith(esFieldsConfig.getNested().getSkus()))
                        .collect(Collectors.groupingBy(ConceptDocDTO::type));

        rootFilters.forEach((field, values) -> {
            List<Query> shouldTerms = values.stream()
                    .map(dto -> Query.of(q -> q.term(t -> t
                            .field(field + "." + esFieldsConfig.getFields().getKeyword())
                            .value(dto.originalTerm())
                    )))
                    .toList();

            if (!shouldTerms.isEmpty()) {
                filterQueries.add(Query.of(q -> q.bool(b -> b
                        .should(shouldTerms)
                )));
            }


        });


    }

    public static void buildNestedFilters(List<ConceptDocDTO> conceptDocDTOList,
                                          List<Query> filterQueries,
                                          EsFieldsConfig esFieldsConfig) {
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
                                    .query(q2 -> q2
                                            .bool(b -> b
                                                    .filter(f ->
                                                            f.bool(sb -> sb.should(nestedSkuQueries))
                                                    )
                                            )
                                    )
                            )
                    )
            );
        }
    }

    public static void addBrandAggregation(
            SearchRequest.Builder searchBuilder,
            ProductRequestDTO request,
            EsFieldsConfig config
    ) {
        searchBuilder.aggregations(
                config.getFields().getBrand(),
                brand -> brand.terms(t -> t
                        .field(config.getFields().getBrandKeyword())
                        .size(request.getValidatedSize(config.getRequest().getDefaultQuerySize()))
                        .order(List.of(
                                NamedValue.of(config.getAggregation().getCount(), SortOrder.Desc),
                                NamedValue.of(config.getAggregation().getKey(), SortOrder.Asc)
                        ))
                )
        );
    }


    public static void addPriceRangeAggregation(
            SearchRequest.Builder searchBuilder,
            EsFieldsConfig config
    ) {
        searchBuilder.aggregations(
                config.getAggregation().getPriceRanges(),
                a -> a.range(r -> r
                        .field(config.getFields().getPrice())
                        .ranges(
                                AggregationRange.of(rb -> rb
                                        .to(config.getAggregation().getCheapPrice())
                                        .key(config.getAggregation().getCheap())
                                ),
                                AggregationRange.of(rb -> rb
                                        .from(config.getAggregation().getCheapPrice())
                                        .to(config.getAggregation().getExpensivePrice())
                                        .key(config.getAggregation().getAverage())
                                ),
                                AggregationRange.of(rb -> rb
                                        .from(config.getAggregation().getExpensivePrice())
                                        .key(config.getAggregation().getExpensive())
                                )
                        )
                )
        );
    }

}
