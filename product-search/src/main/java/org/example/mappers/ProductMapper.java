package org.example.mappers;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.example.dto.FacetBucketDTO;
import org.example.dto.FacetDTO;
import org.example.dto.ProductDTO;
import org.example.dto.ProductResponseDTO;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ProductMapper {
    private final String PRICE_RANGES = "price_ranges";
    private final String BRAND = "brand";


    public List<ProductDTO> mapHitsToProducts(SearchResponse<ProductDTO> response) {
        return response.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .toList();
    }

    public FacetDTO mapAggregationsToFacets(Map<String, Aggregate> aggregations) {
        Map<String, List<FacetBucketDTO>> facets = new HashMap<>();

        var priceAgg = aggregations.get(PRICE_RANGES);
        if (priceAgg != null && priceAgg.isRange()) {
            List<FacetBucketDTO> priceBuckets = priceAgg.range().buckets().array().stream()
                    .map(bucket -> FacetBucketDTO.builder()
                            .value(bucket.key())
                            .count(bucket.docCount())
                            .build())
                    .toList();
            facets.put(PRICE_RANGES, priceBuckets);
        }

        var brandAgg = aggregations.get(BRAND);
        if (brandAgg != null && brandAgg.isSterms()) {
            List<FacetBucketDTO> brandBuckets = brandAgg.sterms().buckets().array().stream()
                    .map(bucket -> FacetBucketDTO.builder()
                            .value(bucket.key().stringValue())
                            .count(bucket.docCount())
                            .build())
                    .toList();
            facets.put(BRAND, brandBuckets);
        }

        return FacetDTO.builder().facetBucketDTO(facets).build();
    }

    public ProductResponseDTO toProductResponseDTO(SearchResponse<ProductDTO> response) {
        List<ProductDTO> products = mapHitsToProducts(response);
        FacetDTO facetDTO = mapAggregationsToFacets(response.aggregations());

        return ProductResponseDTO.builder()
                .totalHits(response.hits().total() != null
                        ? response.hits().total().value()
                        : 0L)
                .productDTOList(products)
                .facetDTO(facetDTO)
                .build();
    }
}
