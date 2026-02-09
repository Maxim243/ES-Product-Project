package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;

@Component
@ConfigurationProperties(prefix = "elasticsearch")
@Data
public class EsFieldsConfig {
    private Fields fields;
    private Property property;
    private Index index;
    private File file;
    private OpenAI openAI;

    @Data
    public static class Fields {
        private String name;
        private String id;
        private String nameVector;
    }

    @Data
    public static class Index {
        private String index;
        private String indexName;
        private Long indicesAmount;
    }

    @Data
    public static class Property {
        private String esHost;
        private String user;
        private String password;
    }

    @Data
    public static class File {
        private Resource mappings;
        private Resource settings;
        private Resource bulkData;
    }

    @Data
    public static class OpenAI {
        private String embeddingModel;
    }
}
