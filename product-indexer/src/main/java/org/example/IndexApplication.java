package org.example;

import lombok.RequiredArgsConstructor;
import org.example.config.EsFieldsConfig;
import org.example.service.IndexService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

@SpringBootApplication(scanBasePackages = {"org.example"})
@RequiredArgsConstructor
public class IndexApplication implements CommandLineRunner {

    private final EsFieldsConfig esFieldsConfig;

    private final IndexService indexService;

    private static final String CREATE_NEW_INDEX_ARG = "createNewIndex";

    public static void main(String[] args) {
        SpringApplication.run(IndexApplication.class, CREATE_NEW_INDEX_ARG);
    }

    @Override
    public void run(String... strings) throws IOException {
        List<String> args = asList(strings);
        boolean needToCreateNewIndex = args.contains(CREATE_NEW_INDEX_ARG);
        if (needToCreateNewIndex) {
            indexService.createIndex();
            indexService.deletePreviousIndices(esFieldsConfig.getIndex().getIndexName(), esFieldsConfig.getIndex().getIndicesAmount());
        }
    }
}
