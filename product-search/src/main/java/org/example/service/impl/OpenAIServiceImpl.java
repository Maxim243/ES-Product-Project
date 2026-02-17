package org.example.service.impl;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.RequiredArgsConstructor;
import org.example.config.EsFieldsConfig;
import org.example.dto.AICandidateDoc;
import org.example.exception.NoContentAISearchException;
import org.example.service.OpenAIService;
import org.example.utils.AIPromptUtil;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.example.utils.JsonUtil.parseIds;

@Service
@RequiredArgsConstructor
public class OpenAIServiceImpl implements OpenAIService {

    private final OpenAIClient openAIClient;

    private final EsFieldsConfig esFieldsConfig;


    @Override
    public List<String> getDocIdsIOpenAI(String userQuery, List<AICandidateDoc> aiCandidateDocs) {
        String aiPrompt = AIPromptUtil.buildAIPrompt(userQuery, aiCandidateDocs);

        ChatCompletionCreateParams params =
                ChatCompletionCreateParams.builder()
                        .model(esFieldsConfig.getOpenAI().getVersion())
                        .temperature(esFieldsConfig.getOpenAI().getTemperature())
                        .maxTokens(esFieldsConfig.getOpenAI().getMaxTokens())
                        .addUserMessage(aiPrompt)
                        .build();

        ChatCompletion completion =
                openAIClient.chat().completions().create(params);

        String content =
                completion.choices().get(0).message().content().orElseThrow(() -> new NoContentAISearchException("No content found"));

        return parseIds(content);
    }
}
