package org.example.service;

import org.example.dto.AICandidateDoc;

import java.util.List;

public interface OpenAIService {

    List<String> getDocIdsIOpenAI(String aiPrompt, List<AICandidateDoc> aiCandidateDocs);
}
