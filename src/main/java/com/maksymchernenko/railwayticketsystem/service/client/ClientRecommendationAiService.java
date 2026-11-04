package com.maksymchernenko.railwayticketsystem.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientRecommendationResponse;
import com.maksymchernenko.railwayticketsystem.service.ai.AiAssistantFactory;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientRecommendationAiService {

    private static final int MAX_OUTPUT_TOKENS = 160;
    private static final String SYSTEM_PROMPT = """
            You generate one passenger-facing railway ticket recommendation.
            Return strict JSON only with fields message and candidateId.
            Do not add markdown, explanations, or text outside JSON.
            """;

    private final ClientRecommendationContextService contextService;
    private final AiAssistantFactory assistantFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClientRecommendationResponse generate(Long userId, String language) {
        ClientRecommendationContextService.RecommendationContext context = contextService.build(userId, language);
        String rawAnswer = askModel(context.prompt());
        return toResponse(rawAnswer, context);
    }

    private String askModel(String prompt) {
        if (!assistantFactory.isGoogleConfigured()) {
            return null;
        }

        return assistantFactory.googleAssistant(RecommendationAi.class, null, 0.75, MAX_OUTPUT_TOKENS)
                .generate(prompt);
    }

    private ClientRecommendationResponse toResponse(
            String rawAnswer,
            ClientRecommendationContextService.RecommendationContext context
    ) {
        RecommendationDraft draft = parse(rawAnswer).orElse(null);
        if (draft == null || !StringUtils.hasText(draft.message())) {
            return context.fallbackResponse();
        }
        if (!context.isExpectedCandidate(draft.candidateId()) || !context.isValidMessage(draft.message())) {
            return context.fallbackResponse();
        }

        return context.actionByCandidateId(draft.candidateId())
                .map(action -> new ClientRecommendationResponse(
                        draft.message().trim(),
                        null,
                        Map.of(),
                        action.type(),
                        action.params()
                ))
                .orElseGet(() -> new ClientRecommendationResponse(draft.message().trim()));
    }

    private Optional<RecommendationDraft> parse(String rawAnswer) {
        if (!StringUtils.hasText(rawAnswer)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(extractJson(rawAnswer), RecommendationDraft.class));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private String extractJson(String rawAnswer) {
        String trimmed = rawAnswer.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        return start >= 0 && end > start ? trimmed.substring(start, end + 1) : trimmed;
    }

    private record RecommendationDraft(String message, String candidateId) {
    }

    private interface RecommendationAi {

        @SystemMessage(SYSTEM_PROMPT)
        String generate(@UserMessage String prompt);
    }
}
