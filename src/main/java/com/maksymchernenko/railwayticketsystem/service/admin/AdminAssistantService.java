package com.maksymchernenko.railwayticketsystem.service.admin;

import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAssistantChatRequest;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAssistantChatResponse;
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
public class AdminAssistantService {

    private static final String SYSTEM_PROMPT = """
            You are the administrator assistant for a railway ticket system.

            Language:
            - Determine the answer language only from the latest administrator message.
            - Ignore the UI language and recent conversation language when choosing the answer language.
            - Keep route numbers, station names, train numbers, dates, currencies, and numeric values exactly as tools return them.
            - Before calling station, route, transfer, connection-window, or analytics tools with station names, normalize names from any user language into the official local spelling used by the railway station in its country.
            - Handle inflected city names, missing diacritics, transliteration, and common city-only names before assuming that a station or route is missing.

            Scope:
            - You may use tools to inspect railway records, route network, selected analytics, explicitly requested analytics, trip load, transfer corridors, connection windows, and service-improvement context.
            - For factual data or recommendations, call tools first. Do not invent routes, demand, revenue, prices, dates, occupancy, or timetable facts.
            - You may give operational recommendations only after tools provide context.

            Recommendation rules:
            - Low passenger flow or low occupancy can justify fewer cars, discounts, timetable changes, or merging weak service.
            - High passenger flow or high occupancy can justify more cars, extra trips, price review, or earlier sales prompts.
            - Popular A-B and B-C corridors can justify a direct A-C route or a better B connection window only when direct A-C demand is weak or absent.
            - For transfer-corridor answers, do not expose internal field names, formulas, or code-like terms such as planningDemandScore, firstLegTickets, secondLegTickets, directDemandTickets, waitMinutes, tripId, or min(...).
            - Explain transfer-corridor evidence in plain language: sales are strong on both connected parts, direct A-C demand is weak or absent, and there is an upcoming timetable connection at the transfer station.
            - Do not collapse transfer-corridor answers into "direct routes already exist". Always list concrete A-B-C corridors and use the actionIdeas returned by the tool.
            - Transfer-corridor tool results are already deduplicated by target A-C direction and suppress candidates where A-C is already strongly demanded. Do not create extra permutations with the same stations in another order.
            - If directRouteAlreadyExists is true, suggest an extra direct trip/time slot, capacity/pricing review, marketing of the direct option, and B-C timetable tuning. If false, suggest adding a direct A-C route.
            - For transfer-corridor wait times, always show the exact connection times from connection.firstArrivalAtTransferLocal and connection.secondDepartureAtTransferLocal. If you do not show those times, do not mention waitMinutes.
            - Use sales trend data before claiming popularity is rising or falling.
            - Separate facts from recommendations when confidence is limited.

            Privacy:
            - Do not reveal personal user data, passenger names, payment identifiers, or individual order details.
            - Aggregate analytics and operational records are allowed for administrators.

            Answer style:
            - Be concise, concrete, and useful.
            - Mention which period and filters were used when analytics matter.
            - When showing timetable times, use fields ending with AtLocal. Do not convert or display fields ending with AtUtc unless the administrator explicitly asks for UTC.
            - Do not print Java/JSON field names, formulas, or backticked technical identifiers in user-facing answers. Translate tool data into business language.
            - Prefer short bullet points. Avoid large tables unless the user explicitly asks for a table.
            """;

    private final AdminAnalyticsTools tools;
    private final AiAssistantFactory assistantFactory;
    private final ChatHistoryService chatHistoryService;
    private final AiErrorSanitizer errorSanitizer;

    @Value("${app.ai.admin-chat.max-output-tokens:2200}")
    private int maxOutputTokens;

    public AdminAssistantChatResponse chat(AdminAssistantChatRequest request) {
        String conversationId = StringUtils.hasText(request.conversationId())
                ? request.conversationId()
                : UUID.randomUUID().toString();
        AdminAnalyticsTools.SelectedAnalyticsFilters filters = new AdminAnalyticsTools.SelectedAnalyticsFilters(
                request.dateFrom(),
                request.dateTo(),
                request.routeId(),
                request.limit()
        );
        String answer = tools.withSelectedAnalyticsFilters(filters, () -> askModel(prompt(request, conversationId)));

        answer = normalize(answer);
        chatHistoryService.remember(conversationId, request.message(), answer);
        return new AdminAssistantChatResponse(conversationId, answer);
    }

    private String askModel(String prompt) {
        if (!assistantFactory.isGoogleConfigured()) {
            return unavailableMessage(null);
        }

        try {
            return assistantFactory.googleAssistant(AdminAiAssistant.class, tools, 0.25, maxOutputTokens)
                    .chat(prompt);
        } catch (RuntimeException exception) {
            return unavailableMessage(exception);
        }
    }

    private String prompt(AdminAssistantChatRequest request, String conversationId) {
        return """
                Current backend time UTC: %s
                UI language setting: %s
                Important: UI language is context only. Do not use it to choose the answer language.

                Answer language:
                - Use the language of the latest administrator message below.
                - Do not copy the language of recent conversation if the latest message uses another language.

                Selected analytics filters from the UI:
                - dateFrom: %s
                - dateTo: %s
                - routeId: %s
                - limit: %s

                Tool guidance:
                - Use getSystemRecordSummary for general questions about how many railway records are configured.
                - Use getSelectedAnalyticsOverview for questions about the currently visible analytics tables.
                - Use getAnalyticsOverview when the administrator asks for another explicit period or route.
                - Use getServiceImprovementContext for recommendations about improving service.
                - Use getTransferDemandContext for A-B plus B-C demand, direct-route ideas, or transfer-corridor questions. For this answer format: list only returned corridors, maximum 5. For each corridor, show the route as "A - B + B - C => A - C", describe demand in business words, show sales on the two parts only as aggregate context when useful, describe direct A-C demand as weak/absent or already meaningful, show route numbers and local connection times at B, then give 1 concrete action idea. Do not show internal score names, formulas, raw trip ids, or Java/JSON field names. If few or no corridors are returned, say in plain language that many candidates were filtered out because direct demand was already strong or no realistic upcoming connection was found.
                - Use getUpcomingTripLoad for adding/removing cars, extra trips, or occupancy questions.
                - Use getConnectionWindows when the administrator asks whether transfers are convenient.
                - In trip-load, transfer-corridor, and connection-window results, show local display times from fields ending with AtLocal. Ignore fields ending with AtUtc for user-facing time text unless UTC is explicitly requested.
                - Use findRoutes or getRouteNetwork for route and station context.
                - Do not rely on the administrator-provided station spelling as the only query. If a station/city name may be written in a different language, first convert it to the local official city/station spelling for the country where that station is located.
                - If a station, route, transfer, or connection-window tool cannot resolve a station name, retry with localized alternatives: main station name, local city name, common transliteration, and spelling with/without diacritics when relevant.
                - When the administrator writes only a city name, search for the main passenger station for that city in its local naming convention.
                - Do not answer that a station, route, or corridor is missing until localized and transliterated search attempts also fail.

                Recent conversation:
                %s

                Latest administrator message:
                %s
                """.formatted(
                Instant.now(),
                StringUtils.hasText(request.language()) ? request.language().trim() : "not specified",
                request.dateFrom(),
                request.dateTo(),
                request.routeId() == null ? "all routes" : request.routeId(),
                request.limit(),
                chatHistoryService.format(conversationId, "Administrator"),
                request.message()
        );
    }

    private String normalize(String answer) {
        return StringUtils.hasText(answer) ? answer.trim() : "AI assistant is unavailable. Analytics tables remain available.";
    }

    private String unavailableMessage(RuntimeException exception) {
        if (exception == null) {
            return "AI assistant is unavailable because Google AI is not configured. Analytics tables remain available.";
        }
        return "AI assistant is unavailable. Details: %s.".formatted(errorSanitizer.sanitize(exception));
    }

    private interface AdminAiAssistant {

        @SystemMessage(SYSTEM_PROMPT)
        String chat(@UserMessage String message);
    }
}
