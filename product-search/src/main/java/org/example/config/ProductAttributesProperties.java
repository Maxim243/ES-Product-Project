package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "products")
public class ProductAttributesProperties {

    private List<String> colors;
    private List<String> sizes;
}
