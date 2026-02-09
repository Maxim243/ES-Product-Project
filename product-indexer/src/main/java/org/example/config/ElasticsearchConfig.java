package org.example.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.micrometer.common.util.StringUtils.isNotBlank;

@Configuration
@RequiredArgsConstructor
@Data
public class ElasticsearchConfig {

    private final EsFieldsConfig esFieldsConfig;

    @Bean(name = "esClient")
    public RestHighLevelClient getEsClient() {
        String user = esFieldsConfig.getProperty().getUser();
        String esHost = esFieldsConfig.getProperty().getEsHost();
        String password = esFieldsConfig.getProperty().getPassword();

        RestClientBuilder restClientBuilder = RestClient.builder(HttpHost.create(esHost));
        if (isNotBlank(user) && isNotBlank(password)) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(user, password));
            restClientBuilder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return new RestHighLevelClient(restClientBuilder);
    }
}