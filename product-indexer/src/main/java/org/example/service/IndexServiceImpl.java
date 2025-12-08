package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static io.micrometer.common.util.StringUtils.isNotEmpty;

@Component
@Slf4j
public class IndexServiceImpl implements IndexService {

    @Autowired
    private RestHighLevelClient esClient;

    @Value("${com.griddynamics.es.graduation.project.index}")
    private String aliasName;

    @Value("${com.griddynamics.es.graduation.project.files.mappings:classpath:elastic/typeaheads/mappings.json}")
    private Resource typeaheadsMappingsFile;

    @Value("${com.griddynamics.es.graduation.project.files.settings:classpath:elastic/typeaheads/settings.json}")
    private Resource typeaheadsSettingsFile;

    @Value("${com.griddynamics.es.graduation.project.files.bulkData:classpath:elastic/typeaheads/bulk_data.txt}")
    private Resource typeaheadsBulkInsertDataFile;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void createIndex() throws IOException {
        String generatedUniqueIndexName = generateUniqueIndexName(aliasName);

        String settings = getStrFromResource(typeaheadsSettingsFile);
        String mappings = getStrFromResource(typeaheadsMappingsFile);
        createIndex(generatedUniqueIndexName, settings, mappings);

        updateIndexAlias(generatedUniqueIndexName);
        processBulkInsertData(typeaheadsBulkInsertDataFile);
        esClient.indices().refresh(new RefreshRequest(generatedUniqueIndexName), RequestOptions.DEFAULT);
    }

    @Override
    public void deletePreviousIndices(String indexPrefix, Long keepIndices) throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest(indexPrefix + "*");

        List<String> allIndices = Arrays.asList(esClient.indices().get(getIndexRequest, RequestOptions.DEFAULT).getIndices());

        List<String> sortedIndices = allIndices
                .stream()
                .sorted(Comparator.reverseOrder())
                .toList();

        List<String> indicesToDelete = sortedIndices
                .stream()
                .skip(keepIndices)
                .toList();

        for (String index : indicesToDelete) {
            esClient.indices().delete(new DeleteIndexRequest(index), RequestOptions.DEFAULT);
        }
    }

    private void updateIndexAlias(String generatedUniqueIndexName) {
        try {
            IndicesAliasesRequest aliasesRequest = new IndicesAliasesRequest();
            removeAliasesAction(aliasesRequest);
            addAliasAction(aliasesRequest, generatedUniqueIndexName);
            esClient.indices().updateAliases(aliasesRequest, RequestOptions.DEFAULT);
        } catch (IOException | ElasticsearchException e) {
            deleteIndex(generatedUniqueIndexName);
        }
    }

    private void removeAliasesAction(IndicesAliasesRequest indicesAliasesRequest) throws IOException {
        GetAliasesResponse response = esClient.indices().getAlias(
                new GetAliasesRequest(aliasName), RequestOptions.DEFAULT);

        AliasMetaData aliasMetaData = AliasMetaData
                .newAliasMetaDataBuilder(aliasName)
                .build();

        List<String> indicesToBeCleared = response.getAliases().entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(aliasMetaData))
                .map(Map.Entry::getKey)
                .toList();

        if (!indicesToBeCleared.isEmpty()) {
            IndicesAliasesRequest.AliasActions removeAction = new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
                    .indices(indicesToBeCleared.toArray(new String[0]))
                    .alias(aliasName);

            indicesAliasesRequest.addAliasAction(removeAction);
        }
    }

    private void addAliasAction(IndicesAliasesRequest indicesAliasesRequest, String generatedUniqueIndexName) {
        IndicesAliasesRequest.AliasActions addAction =
                new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
                        .index(generatedUniqueIndexName)
                        .alias(aliasName);

        indicesAliasesRequest.addAliasAction(addAction);
    }



    private String generateUniqueIndexName(String generalName) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return generalName + "_" + now.format(formatter);
    }

    private static String getStrFromResource(Resource resource) {
        try {
            if (!resource.exists()) {
                throw new IllegalArgumentException("File not found: " + resource.getFilename());
            }
            return Resources.toString(resource.getURL(), Charsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Can not read resource file: " + resource.getFilename(), ex);
        }
    }

    private void processBulkInsertData(Resource bulkInsertDataFile) {
        int requestCnt = 0;
        try {
            BulkRequest bulkRequest = new BulkRequest();
            BufferedReader br = new BufferedReader(new InputStreamReader(bulkInsertDataFile.getInputStream()));

            while (br.ready()) {
                String line1 = br.readLine(); // action_and_metadata
                if (isNotEmpty(line1) && br.ready()) {
                    requestCnt++;
                    String line2 = br.readLine();
                    IndexRequest indexRequest = createIndexRequestFromBulkData(line1, line2);
                    if (indexRequest != null) {
                        bulkRequest.add(indexRequest);
                    }
                }
            }

            BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.getItems().length != requestCnt) {
                log.warn("Only {} out of {} requests have been processed in a bulk request.", bulkResponse.getItems().length, requestCnt);
            } else {
                log.info("{} requests have been processed in a bulk request.", bulkResponse.getItems().length);
            }

            if (bulkResponse.hasFailures()) {
                log.warn("Bulk data processing has failures:\n{}", bulkResponse.buildFailureMessage());
            }
        } catch (IOException ex) {
            log.error("An exception occurred during bulk data processing", ex);
            throw new RuntimeException(ex);
        }
    }
    private IndexRequest createIndexRequestFromBulkData(String line1, String line2) {
        DocWriteRequest.OpType opType = null;
        String esIndexName = null;
        String esId = null;
        boolean isOk = true;

        try {
            String esOpType = objectMapper.readTree(line1).fieldNames().next();
            opType = DocWriteRequest.OpType.fromString(esOpType);

            JsonNode indexJsonNode = objectMapper.readTree(line1).iterator().next().get("_index");
            esIndexName = (indexJsonNode != null ? indexJsonNode.textValue() : aliasName);

            JsonNode idJsonNode = objectMapper.readTree(line1).iterator().next().get("_id");
            esId = (idJsonNode != null ? idJsonNode.textValue() : null);
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("An exception occurred during parsing action_and_metadata line in the bulk data file:\n{}\nwith a message:\n{}", line1, ex.getMessage());
            isOk = false;
        }

        try {
            objectMapper.readTree(line2);
        } catch (IOException ex) {
            log.warn("An exception occurred during parsing source line in the bulk data file:\n{}\nwith a message:\n{}", line2, ex.getMessage());
            isOk = false;
        }

        if (isOk) {
            return new IndexRequest(esIndexName)
                    .id(esId)
                    .opType(opType)
                    .source(line2, XContentType.JSON);
        } else {
            return null;
        }
    }


    private void createIndex(String indexName, String settings, String mappings) {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName)
                .mapping(mappings, XContentType.JSON)
                .settings(settings, XContentType.JSON);

        CreateIndexResponse createIndexResponse;
        try {
            createIndexResponse = esClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        } catch (IOException ex) {
            throw new RuntimeException("An error occurred during creating ES index.", ex);
        }

        if (!createIndexResponse.isAcknowledged()) {
            throw new RuntimeException("Creating index not acknowledged for indexName: " + indexName);
        } else {
            log.info("Index {} has been created.", indexName);
        }
    }

    private void deleteIndex(String indexName) {
        try {
            DeleteIndexRequest deleteRequest = new DeleteIndexRequest(indexName);
            AcknowledgedResponse acknowledgedResponse = esClient.indices().delete(deleteRequest, RequestOptions.DEFAULT);
            if (!acknowledgedResponse.isAcknowledged()) {
                log.warn("Index deletion is not acknowledged for indexName: {}", indexName);
            } else {
                log.info("Index {} has been deleted.", indexName);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Deleting of old index version is failed for indexName: " + indexName, ex);
        }
    }
}
