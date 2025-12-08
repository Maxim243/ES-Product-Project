package org.example.service;

import java.io.IOException;

public interface IndexService {

    void createIndex() throws IOException;
    void deletePreviousIndices(String indexPrefix, Long keepIndices) throws IOException;
}
