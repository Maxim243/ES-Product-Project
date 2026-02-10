package org.example.config;

import lombok.Data;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static io.micrometer.common.util.StringUtils.isNotBlank;

@Configuration
@Data
public class ElasticsearchConfig {

    @Value("${com.griddynamics.es.graduation.project.esHost}")
    private String esHost;

    @Value("${com.griddynamics.es.graduation.project.user}")
    private String user;

    @Value("${com.griddynamics.es.graduation.project.pass}")
    private String pass;

    @Bean(name = "esClient")
    public RestHighLevelClient getEsClient() {
        RestClientBuilder restClientBuilder = RestClient.builder(HttpHost.create(esHost));
        if (isNotBlank(user) && isNotBlank(pass)) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(user, pass));
            restClientBuilder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return new RestHighLevelClient(restClientBuilder);
    }
}