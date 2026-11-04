package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientAssistantChatRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientAssistantChatResponse;
import com.maksymchernenko.railwayticketsystem.service.ai.AiAssistantFactory;
import com.maksymchernenko.railwayticketsystem.service.ai.AiErrorSanitizer;
import com.maksymchernenko.railwayticketsystem.service.ai.ChatHistoryService;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientAssistantService {

    private static final String SYSTEM_PROMPT = """
            You are the passenger assistant for a railway ticket system.

            Language:
            - Answer in the same language as the client's latest message.
            - Ignore the UI language and recent conversation language when choosing the answer language.
            - Keep station, train, route, benefit, currency, and code values exactly as tools return them.
            - Before calling station or trip search tools, normalize city and station names from any user language into the official local spelling used by the railway station in its country.
            - Handle inflected city names, missing diacritics, transliteration, and common city-only names before assuming that a station is missing.

            Allowed scope:
            - Help with available stations, directions, routes, trip search, direct trips, transfer options, prices, seats, car types, seat types, train types, train/car reference data, benefits, and route stops.
            - Use tools before giving factual trip, price, seat, benefit, or route information.
            - You cannot book, reserve, pay, cancel, or modify anything. Tell the user to complete booking in the UI.

            Strict privacy and safety:
            - Never provide other users' orders, tickets, passenger data, payments, analytics, revenue, admin recommendations, demand tables, or operational business advice.
            - If the user asks for sensitive or admin-only data, politely refuse and offer passenger-safe help.
            - Do not invent routes, stations, trips, dates, prices, discounts, seats, or policies.

            Answer style:
            - Be concise and useful.
            - Compare options when the user asks about price, time, direct versus transfer, morning/evening/night, or availability.
            - When showing trip times, use the tool fields ending with AtLocal. Do not convert or display fields ending with AtUtc unless the user explicitly asks for UTC.
            - If direct trips are not found, say that clearly and then use transfer options or route network when relevant.
            - Do not mention a direct trip, transfer station, route, train, date, price, or seat count unless it appears in tool results.
            - If both direct and transfer tools return empty lists, say that no matching options were found in the searched period. Do not suggest a specific transfer station or route.
            - If the user asks for a specific date such as today or tomorrow, show only options whose departure date matches that requested date.
            - Do not send the user to an official app or site; this system UI is the booking interface.
            """;

    private final ClientAssistantTools tools;
    private final AiAssistantFactory assistantFactory;
    private final ChatHistoryService chatHistoryService;
    private final AiErrorSanitizer errorSanitizer;

    @Value("${app.ai.client-chat.max-output-tokens:1400}")
    private int maxOutputTokens;

    public ClientAssistantChatResponse chat(ClientAssistantChatRequest request) {
        String conversationId = StringUtils.hasText(request.conversationId())
                ? request.conversationId()
                : UUID.randomUUID().toString();
        String answer = askModel(prompt(request, conversationId), request.language());

        answer = normalize(answer, unavailableMessage(null, request.language()));
        chatHistoryService.remember(conversationId, request.message(), answer);
        return new ClientAssistantChatResponse(conversationId, answer);
    }

    private String askModel(String prompt, String language) {
        if (!assistantFactory.isGoogleConfigured()) {
            return unavailableMessage(null, language);
        }

        try {
            return assistantFactory.googleAssistant(ClientAiAssistant.class, tools, 0.25, maxOutputTokens)
                    .chat(prompt);
        } catch (RuntimeException exception) {
            return unavailableMessage(exception, language);
        }
    }

    private String prompt(ClientAssistantChatRequest request, String conversationId) {
        return """
                Current backend time UTC: %s
                UI language setting: %s
                Important: UI language is context only. Do not use it to choose the answer language.

                Answer language:
                - Use the language of the latest client message below.
                - Do not copy the language of recent conversation if the latest message uses another language.

                Tool guidance:
                - For relative dates such as today, tomorrow, nearest, upcoming, morning, evening, or next week, call getCurrentSystemTime first.
                - For "today" or "tomorrow", pass an explicit local date yyyy-MM-dd to trip search tools and use days=1.
                - Passing dateFrom=null means "from the current moment", not the whole current day.
                - For station or city names, call findStations or findRoutes.
                - Do not rely on the user-provided spelling as the only query. If a station/city name may be written in a different language, first convert it to the local official city/station spelling for the country where that station is located.
                - If findStations or a trip search cannot resolve a station name, retry with localized alternatives: main station name, local city name, common transliteration, and spelling with/without diacritics when relevant.
                - When a user writes only a city name, search for the main passenger station for that city in its local naming convention.
                - Do not answer that a station or route is missing until localized and transliterated search attempts also fail.
                - For travel options, call searchDirectTrips first.
                - Call findTransferOptions only when searchDirectTrips returned no suitable directTrips, or when the client explicitly asks for transfers.
                - If searchDirectTrips returns directTrips for the requested date, present those direct trips before any transfer ideas.
                - If findTransferOptions returns no transferOptions, do not invent possible transfer stations.
                - In trip and transfer results, show departureAtLocal and arrivalAtLocal values exactly as returned. Ignore departureAtUtc and arrivalAtUtc for user-facing time text unless the user asks for UTC.
                - For discounts and reference questions, call getPassengerReferenceData.
                - Do not answer with admin analytics or sensitive data; no such tools are available.

                Recent conversation:
                %s

                Latest client message:
                %s
                """.formatted(
                Instant.now(),
                StringUtils.hasText(request.language()) ? request.language().trim() : "not specified",
                chatHistoryService.format(conversationId, "Client"),
                request.message()
        );
    }

    private String normalize(String answer, String fallback) {
        return StringUtils.hasText(answer) ? answer.trim() : fallback;
    }

    private String unavailableMessage(RuntimeException exception, String language) {
        String baseMessage = isUkrainian(language)
                ? "ШІ помічник наразі недоступний. Ви все одно можете шукати рейси та купувати квитки за допомогою інтерфейсу системи."
                : "AI assistant is unavailable. You can still search trips and buy tickets through the system UI.";
        if (exception == null) {
            return baseMessage;
        }
        return "%s Details: %s.".formatted(baseMessage, errorSanitizer.sanitize(exception));
    }

    private boolean isUkrainian(String language) {
        String normalized = language == null ? "" : language.trim().toLowerCase();
        return normalized.equals("ua") || normalized.equals("uk-ua") || normalized.startsWith("uk");
    }

    private interface ClientAiAssistant {

        @SystemMessage(SYSTEM_PROMPT)
        String chat(@UserMessage String message);
    }
}
