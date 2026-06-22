package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientAssistantChatRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientAssistantChatResponse;
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
public class ClientAssistantService {

    private static final int MAX_HISTORY_TURNS = 6;
    private static final String SYSTEM_PROMPT = """
            You are an AI assistant for railway ticket system clients.
            Use tools to get real backend data before recommending routes, trips, tickets, transfers, prices, seats, or discounts.
            Return only the final answer for the current client message. Never answer with interim phrases like "I will check", "please wait", "зачекайте", or "я перевірю", because the chat will not send a second assistant message automatically.
            Answer in the same language as the client's latest manual message. The latest manual client message is more important for language choice than this system prompt, previous messages, or tool data.
            Give practical, concise recommendations: nearest trips, morning/evening/night options, cheapest options, seat availability, direct route availability, transfer alternatives, and relevant benefits.
            The user may write a city instead of the full station name. Use the provided station and route catalog to choose the closest exact station or route name from the database before calling tools.
            A route may be relevant even when the requested stations are not its endpoints, as long as both stations are present in the requested travel order.
            Do not invent routes, stations, trips, discounts, prices, dates, seat availability, or policies. If tools do not provide enough context, clearly say that you cannot complete the exact task and offer available alternatives such as searching nearby dates, checking direct routes, transfer options, available benefits, or asking the client to clarify station/date.
            If a client cannot buy a ticket, check direct trips first, then route network or transfers. Explain whether there is no direct route, no scheduled upcoming trip, no seats, or insufficient data.
            Do not ask for or reveal personal data, payment data, tokens, passwords, internal IDs unless they are trip or route identifiers needed for booking.
            You cannot create bookings, reserve seats, sell tickets, modify orders, or change system data. Do not ask whether the client wants you to book or buy a ticket. Instead, after giving route or trip options, mention that booking, seat selection, and purchase must be completed in the system UI.
            Prefer short bullet lists when comparing multiple options. Mention current backend time when answering about nearest or upcoming trips.
            """;

    private final ClientAssistantTools clientAssistantTools;
    private final Map<String, Deque<ChatTurn>> conversationHistory = new ConcurrentHashMap<>();

    @Value("${app.ai.google-api-key:}")
    private String googleApiKey;

    @Value("${app.ai.model:gemini-2.5-flash-lite}")
    private String modelName;

    private volatile ClientAiAssistant assistant;
    private volatile String assistantModelName;

    public ClientAssistantChatResponse chat(ClientAssistantChatRequest request) {
        String conversationId = StringUtils.hasText(request.conversationId())
                ? request.conversationId()
                : UUID.randomUUID().toString();

        if (!StringUtils.hasText(googleApiKey)) {
            return new ClientAssistantChatResponse(conversationId, configurationMissingMessage(request.message()));
        }

        String prompt = buildPrompt(request, conversationId);
        String answer;
        try {
            answer = getAssistant().chat(prompt);
        } catch (RuntimeException exception) {
            log.warn("Client AI assistant request failed. model={}", modelName, exception);
            answer = temporaryFailureMessage(request.message(), exception);
        }

        remember(conversationId, request.message(), answer);
        return new ClientAssistantChatResponse(conversationId, answer);
    }

    private ClientAiAssistant getAssistant() {
        ClientAiAssistant localAssistant = assistant;
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

                assistant = AiServices.builder(ClientAiAssistant.class)
                        .chatModel(chatModel)
                        .tools(clientAssistantTools)
                        .build();
                assistantModelName = modelName;
            }
            return assistant;
        }
    }

    private String buildPrompt(ClientAssistantChatRequest request, String conversationId) {
        return """
                Current backend time UTC: %s

                Station and route catalog:
                %s

                Recent conversation:
                %s

                Latest client message:
                %s
                """.formatted(
                Instant.now(),
                clientAssistantTools.getCatalogPromptText(),
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
            builder.append("Client: ").append(turn.userMessage()).append('\n');
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

    private String configurationMissingMessage(String message) {
        if (looksUkrainian(message)) {
            return "ШІ асистент не налаштований: на бекенді відсутній GOOGLE_AI_API_KEY. Наразі можу допомогти через звичайний пошук рейсів, вибір місць і перегляд квитків у системі.";
        }
        return "AI assistant is not configured: GOOGLE_AI_API_KEY is missing on the backend. You can still use trip search, seat selection, and ticket pages in the system.";
    }

    private String temporaryFailureMessage(String userMessage, RuntimeException exception) {
        String details = sanitizeError(exception);
        if (looksUkrainian(userMessage)) {
            return "Не вдалося отримати відповідь від Google AI API для моделі " + modelName
                    + ". Деталі: " + details
                    + ". Спробуйте пізніше або скористайтеся пошуком рейсів, вибором місць чи сторінкою квитків.";
        }
        return "Could not get a response from Google AI API for model " + modelName
                + ". Details: " + details
                + ". Try again later or use trip search, seat selection, or ticket pages.";
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

    private boolean looksUkrainian(String message) {
        return message != null && message.matches(".*[А-Яа-яЄєІіЇїҐґ].*");
    }

    private record ChatTurn(String userMessage, String assistantAnswer) {
    }

    private interface ClientAiAssistant {

        @SystemMessage(SYSTEM_PROMPT)
        String chat(@UserMessage String message);
    }
}
