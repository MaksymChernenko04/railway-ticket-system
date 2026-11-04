package com.maksymchernenko.railwayticketsystem.service.ai;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiAssistantFactory {

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Value("${app.ai.google-api-key:}")
    private String googleApiKey;

    @Value("${app.ai.model:gemini-3.1-flash-lite}")
    private String modelName;

    public boolean isGoogleConfigured() {
        return StringUtils.hasText(googleApiKey);
    }

    public String googleModelName() {
        return modelName;
    }

    @SuppressWarnings("unchecked")
    public <T> T googleAssistant(Class<T> type, Object tools, double temperature, Integer maxOutputTokens) {
        String key = assistantKey("google", type, tools, modelName, temperature, maxOutputTokens);

        return (T) cache.computeIfAbsent(key, ignored -> {
            var modelBuilder = GoogleAiGeminiChatModel.builder()
                    .apiKey(googleApiKey)
                    .modelName(modelName)
                    .temperature(temperature)
                    .thinkingConfig(GeminiThinkingConfig.builder()
                            .includeThoughts(true)
                            .thinkingLevel(GeminiThinkingConfig.GeminiThinkingLevel.MINIMAL)
                            .build())
                    .returnThinking(true)
                    .sendThinking(true);

            if (maxOutputTokens != null) {
                modelBuilder.maxOutputTokens(maxOutputTokens);
            }

            var serviceBuilder = AiServices.builder(type)
                    .chatModel(modelBuilder.build());
            if (tools != null) {
                serviceBuilder.tools(tools);
            }
            return serviceBuilder.build();
        });
    }

    private String assistantKey(
            String provider,
            Class<?> type,
            Object tools,
            String modelKey,
            double temperature,
            Integer maxOutputTokens
    ) {
        String toolsKey = tools == null
                ? "none"
                : tools.getClass().getName() + "@" + System.identityHashCode(tools);
        return provider + "|" + type.getName() + "|" + toolsKey + "|" + modelKey + "|" + temperature + "|" + maxOutputTokens;
    }
}
