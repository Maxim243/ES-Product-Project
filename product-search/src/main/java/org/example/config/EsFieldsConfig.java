package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "elasticsearch")
@Data
public class EsFieldsConfig {

    private Request request;
    private Fields fields;
    private Aggregation aggregation;
    private Index index;
    private Nested nested;

    @Data
    public static class Request {
        private Integer defaultQuerySize;
        private Integer defaultQueryPage;
        private Integer getAllSize;
    }

    @Data
    public static class Fields {
        private String name;
        private String nameShingles;
        private String brandKeyword;
        private String price;
        private String brand;
        private String keyword;
    }

    @Data
    public static class Aggregation {
        private String count;
        private String key;
        private String cheap;
        private String average;
        private String expensive;
        private String priceRanges;
        private String brandCount;
        private Double cheapPrice;
        private Double expensivePrice;
    }

    @Data
    public static class Index {
        private String productIndex;
        private String conceptIndex;
        private String searchTerms;
    }

    @Data
    public static class Nested {
        private String skus;
    }
}
