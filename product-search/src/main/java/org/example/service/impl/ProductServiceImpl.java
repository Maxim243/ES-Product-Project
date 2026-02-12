package org.example.service.impl;

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
import org.example.enums.QueryType;
import org.example.enums.SearchMessage;
import org.example.exception.SearchServiceUnavailableException;
import org.example.mappers.ProductMapper;
import org.example.service.ProductService;
import org.example.utils.QueryUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.example.dto.ProductResponseDTO.buildEmptyProductResponseDTO;
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

        List<String> textQueryInputTerms = List.of(productRequestDTO.queryText().toLowerCase().split(" "));

        SearchResponse<ConceptDocDTO> conceptDocSearchResponse = getConceptDocSearchResponse(textQueryInputTerms);

        List<ConceptDocDTO> conceptDocDTOList = conceptDocSearchResponse.hits()
                .hits()
                .stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .toList();

        String productNameFieldTokens = extractProductNameFieldTokens(textQueryInputTerms, conceptDocDTOList);

        List<Query> filterQueries = QueryUtil.createFilterQuery(conceptDocDTOList, esFieldsConfig);
        List<Query> mustQueries = QueryUtil.createMustQuery(productNameFieldTokens, esFieldsConfig.getFields().getName());
        List<Query> shouldQueries = QueryUtil.createShouldQuery(productNameFieldTokens, esFieldsConfig.getFields().getNameShingles());

        return trySearchStage(
                QueryType.STRICT,
                productRequestDTO,
                filterQueries,
                mustQueries,
                shouldQueries,
                SearchMessage.STRICT_SUCCESS.getMessage())
                .or(() -> trySearchStage(
                        QueryType.NO_FILTERS,
                        productRequestDTO,
                        filterQueries,
                        mustQueries,
                        shouldQueries,
                        SearchMessage.FILTERS_REMOVED.getMessage()
                ))
                .orElse(buildEmptyProductResponseDTO());
    }


    private Optional<ProductResponseDTO> trySearchStage(
            QueryType queryType,
            ProductRequestDTO productRequestDTO,
            List<Query> filterQueries,
            List<Query> mustQueries,
            List<Query> shouldQueries,
            String successMessage
    ) {

            ProductResponseDTO response = searchProductByStages(
                    queryType,
                    productRequestDTO,
                    filterQueries,
                    mustQueries,
                    shouldQueries
            );

            if (response.getProductDTOList().isEmpty()) {
                return Optional.empty();
            }

            response.setMessage(successMessage);
            return Optional.of(response);
    }

    private SearchResponse<ProductDTO> searchProducts(ProductRequestDTO productRequestDTO, Query mainQuery) {
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index(esFieldsConfig.getIndex().getProductIndex())
                .from(productRequestDTO.from(esFieldsConfig.getRequest().getDefaultQuerySize(), esFieldsConfig.getRequest().getDefaultQueryPage()))
                .size(productRequestDTO.getValidatedSize(esFieldsConfig.getRequest().getDefaultQuerySize()))
                .query(mainQuery)
                .sort(so -> so.score(ss -> ss.order(SortOrder.Desc)));

        addBrandAggregation(searchBuilder, productRequestDTO, esFieldsConfig);
        addPriceRangeAggregation(searchBuilder, esFieldsConfig);

        try {
            return elasticsearchClient.search(searchBuilder.build(), ProductDTO.class);
        } catch (IOException e) {
            log.error("Search stage failed", e);
            throw new SearchServiceUnavailableException(e.getMessage());
        }
    }

    private ProductResponseDTO searchProductByStages(QueryType queryType,
                                                     ProductRequestDTO productRequestDTO,
                                                     List<Query> filterQueries,
                                                     List<Query> mustQueries,
                                                     List<Query> shouldQueries) {

        Query queryByStrategy = QueryUtil.buildQueryByStrategy(queryType, filterQueries, mustQueries, shouldQueries);
        log.info("queryByStrategy: {}", queryByStrategy);

        SearchResponse<ProductDTO> productDTOSearchFirstStage = searchProducts(productRequestDTO, queryByStrategy);
        return productMapper.toProductResponseDTO(productDTOSearchFirstStage);
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