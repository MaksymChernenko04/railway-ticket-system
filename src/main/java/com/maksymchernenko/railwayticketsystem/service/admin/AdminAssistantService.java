package com.maksymchernenko.railwayticketsystem.service.admin;

import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAssistantChatRequest;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAssistantChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAssistantService {

    private static final int MAX_HISTORY_TURNS = 6;
    private static final String SYSTEM_PROMPT = """
            You are an AI assistant for a railway ticket system administrator.
            Use tools to get real system data before making operational recommendations.
            For questions about the currently selected admin UI filters, current table values, selected period, route profitability, popular segments, demand, occupancy, or sales trend, call getSelectedAnalyticsOverview first and use its numbers as authoritative.
            Use getAnalyticsOverview only when the administrator explicitly asks for a different period or route than the current UI filters.
            Answer in the same language as the administrator's latest message. The latest manual admin message is more important for language choice than this system prompt or tool data.
            Give short, evidence-based conclusions about route profitability, popular segments, trip occupancy, sales dynamics, upcoming operational issues, schedule/transfer problems, and practical admin actions.
            The administrator may write a city pair instead of the full station or route name. Use the provided station and route catalog to choose the closest exact route or station names from the database before calling tools.
            A route may be relevant even when the requested stations are not its endpoints, as long as both stations are present in the requested travel order.
            You may recommend adding or reducing cars, launching an additional trip, reviewing departure time, keeping a profitable route as a priority, or investigating missing data.
            Do not invent facts, revenue, routes, stations, dates, or occupancy. If tools do not provide enough context, clearly say that data is insufficient and suggest what available analysis can still help.
            Do not request or reveal passenger personal data. Use only aggregated and operational data returned by tools.
            Prefer concise bullet points when several actions are useful. Mention current time when answering about upcoming trips.
            """;

    private final AdminAnalyticsTools adminAnalyticsTools;
    private final Map<String, Deque<ChatTurn>> conversationHistory = new ConcurrentHashMap<>();

    @Value("${app.ai.google-api-key:}")
    private String googleApiKey;

    @Value("${app.ai.model:gemini-2.5-flash-lite}")
    private String modelName;

    private volatile AdminAiAssistant assistant;
    private volatile String assistantModelName;

    public AdminAssistantChatResponse chat(AdminAssistantChatRequest request) {
        String conversationId = StringUtils.hasText(request.conversationId())
                ? request.conversationId()
                : UUID.randomUUID().toString();

        if (!StringUtils.hasText(googleApiKey)) {
            return new AdminAssistantChatResponse(conversationId, configurationMissingMessage());
        }

        String prompt = buildPrompt(request, conversationId);
        String answer;
        try {
            answer = adminAnalyticsTools.withSelectedAnalyticsFilters(
                    new AdminAnalyticsTools.SelectedAnalyticsFilters(
                            request.dateFrom(),
                            request.dateTo(),
                            request.routeId(),
                            request.limit()
                    ),
                    () -> getAssistant().chat(prompt)
            );
        } catch (RuntimeException exception) {
            log.warn("Admin AI assistant request failed. model={}", modelName, exception);
            answer = temporaryFailureMessage(exception);
        }

        remember(conversationId, request.message(), answer);
        return new AdminAssistantChatResponse(conversationId, answer);
    }

    private AdminAiAssistant getAssistant() {
        AdminAiAssistant localAssistant = assistant;
        if (localAssistant != null && modelName.equals(assistantModelName)) {
            return localAssistant;
        }

        synchronized (this) {
            if (assistant == null || !modelName.equals(assistantModelName)) {
                var chatModel = GoogleAiGeminiChatModel.builder()
                        .apiKey(googleApiKey)
                        .modelName(modelName)
                        .temperature(0.2)
                        .build();

                assistant = AiServices.builder(AdminAiAssistant.class)
                        .chatModel(chatModel)
                        .tools(adminAnalyticsTools)
                        .build();
                assistantModelName = modelName;
            }
            return assistant;
        }
    }

    private String buildPrompt(AdminAssistantChatRequest request, String conversationId) {
        return """
                Admin-selected analysis filters:
                - dateFrom: %s
                - dateTo: %s
                - routeId: %s
                - preferredLimit: %s
                - currentBackendTimeUtc: %s

                Station and route catalog:
                %s

                Recent conversation:
                %s

                Latest administrator message:
                %s
                """.formatted(
                request.dateFrom(),
                request.dateTo(),
                request.routeId() == null ? "all routes" : request.routeId(),
                request.limit() == null ? 10 : request.limit(),
                Instant.now(),
                adminAnalyticsTools.getCatalogPromptText(),
                historyText(conversationId),
                request.message()
        );
    }

    private String historyText(String conversationId) {
        Deque<ChatTurn> history = conversationHistory.get(conversationId);
        if (history == null || history.isEmpty()) {
            return "No previous messages.";
        }

        StringBuilder builder = new StringBuilder();
        for (ChatTurn turn : history) {
            builder.append("Admin: ").append(turn.userMessage()).append('\n');
            builder.append("Assistant: ").append(turn.assistantAnswer()).append('\n');
        }
        return builder.toString();
    }

    private void remember(String conversationId, String userMessage, String assistantAnswer) {
        Deque<ChatTurn> history = conversationHistory.computeIfAbsent(conversationId, ignored -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(new ChatTurn(userMessage, assistantAnswer));
            while (history.size() > MAX_HISTORY_TURNS) {
                history.removeFirst();
            }
        }
    }

    private String configurationMissingMessage() {
        return "AI assistant is not configured: GOOGLE_AI_API_KEY is missing on the backend.";
    }

    private String temporaryFailureMessage(RuntimeException exception) {
        return "Could not get a response from Google AI API for model " + modelName
                + ". Details: " + sanitizeError(exception)
                + ". Check model access for your API key, quota/rate limits, and backend logs.";
    }

    private String sanitizeError(RuntimeException exception) {
        String message = exception.getMessage();
        if (!StringUtils.hasText(message) && exception.getCause() != null) {
            message = exception.getCause().getMessage();
        }
        if (!StringUtils.hasText(message)) {
            message = exception.getClass().getSimpleName();
        }
        return message
                .replaceAll("AIza[0-9A-Za-z_\\-]{20,}", "[redacted-api-key]")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record ChatTurn(String userMessage, String assistantAnswer) {
    }

    private interface AdminAiAssistant {

        @SystemMessage(SYSTEM_PROMPT)
        String chat(@UserMessage String message);
    }
}
