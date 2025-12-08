package org.example;

import org.example.service.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

@SpringBootApplication(scanBasePackages = {"org.example"})
public class IndexApplication implements CommandLineRunner {

    private static final String CREATE_NEW_INDEX_ARG = "createNewIndex";

    @Value("${com.griddynamics.es.graduation.project.index}")
    private String prefixIndexName;

    @Value("${com.griddynamics.es.graduation.project.indices_amount}")
    private Long keepIndicesAmount;


    @Autowired
    IndexService indexService;

    public static void main(String[] args) {
        SpringApplication.run(IndexApplication.class, CREATE_NEW_INDEX_ARG);
    }

    @Override
    public void run(String... strings) throws IOException {
        List<String> args = asList(strings);
        boolean needToCreateNewIndex = args.contains(CREATE_NEW_INDEX_ARG);
        if (needToCreateNewIndex) {
            indexService.createIndex();
            indexService.deletePreviousIndices(prefixIndexName, keepIndicesAmount);
        }
    }
}
