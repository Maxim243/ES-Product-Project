package org.example.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Data;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class ElasticsearchConfig {

    private final EsFieldsConfig esFieldsConfig;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        String user = esFieldsConfig.getProperty().getUser();
        String password = esFieldsConfig.getProperty().getPassword();
        String esHost = esFieldsConfig.getProperty().getEsHost();

        RestClientBuilder builder = RestClient.builder(org.apache.http.HttpHost.create(esHost));

        if (user != null && !user.isBlank() && password != null && !password.isBlank()) {
            final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        }

        RestClient restClient = builder.build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}

