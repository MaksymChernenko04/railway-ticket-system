package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientRecommendationResponse;
import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientRecommendationContextService {

    private static final int RECENT_TICKET_LIMIT = 30;
    private static final int DEMAND_LIMIT = 24;
    private static final int POPULAR_SEGMENT_LIMIT = 30;
    private static final int MAX_CANDIDATES = 12;
    private static final int MAX_MESSAGE_LENGTH = 240;
    private static final String ACTION_TYPE = "trip_search";
    private static final String FALLBACK_MESSAGE_EN = "Choose a route and date to find the most convenient available trip.";
    private static final String FALLBACK_MESSAGE_UK = "Виберіть напрямок та дату, щоб знайти найзручніший доступний рейс.";
    private static final List<Ticket.Status> SOLD_TICKET_STATUSES = List.of(Ticket.Status.ACTIVE, Ticket.Status.FINISHED);

    private final TicketRepository ticketRepository;
    private final TripRepository tripRepository;

    @Transactional(readOnly = true)
    public RecommendationContext build(Long userId, String language) {
        Instant now = Instant.now();
        List<Ticket> tickets = loadRecentTickets(userId);
        List<TripRepository.TripOccupancyRow> demandRows = loadUpcomingDemand(now);
        List<TicketRepository.PopularSegmentRow> popularSegments = loadPopularSegments(now);

        List<RecommendationSignal> historySignals = new ArrayList<>();
        List<RecommendationSignal> demandSignals = new ArrayList<>();
        List<RecommendationSignal> popularSignals = new ArrayList<>();
        List<RecommendationSignal> calendarSignals = new ArrayList<>();

        addHistorySignals(historySignals, tickets, now);
        addDemandSignals(demandSignals, demandRows);
        addPopularSegmentSignals(popularSignals, popularSegments, now);
        addSeasonSignals(calendarSignals, popularSegments, now);
        addCalendarSignals(calendarSignals, popularSegments, now);

        List<RecommendationSignal> candidates = assignCandidateIds(candidateSignals(List.of(
                historySignals,
                calendarSignals,
                demandSignals,
                popularSignals
        ), now));

        return new RecommendationContext(
                prompt(candidates, language, now),
                candidates,
                fallback(candidates.getFirst(), language)
        );
    }

    private List<Ticket> loadRecentTickets(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return ticketRepository.findVisibleTicketsByUser(
                userId,
                Order.Status.PAID,
                PageRequest.of(0, RECENT_TICKET_LIMIT)
        ).getContent();
    }

    private List<TripRepository.TripOccupancyRow> loadUpcomingDemand(Instant now) {
        return tripRepository.findHighestOccupancyTrips(
                now.plus(2, ChronoUnit.HOURS),
                now.plus(14, ChronoUnit.DAYS),
                null,
                Order.Status.PAID,
                SOLD_TICKET_STATUSES,
                PageRequest.of(0, DEMAND_LIMIT)
        );
    }

    private List<TicketRepository.PopularSegmentRow> loadPopularSegments(Instant now) {
        return ticketRepository.findPopularSegments(
                now.minus(90, ChronoUnit.DAYS),
                now,
                null,
                Order.Status.PAID,
                SOLD_TICKET_STATUSES,
                PageRequest.of(0, POPULAR_SEGMENT_LIMIT)
        );
    }

    private void addHistorySignals(List<RecommendationSignal> signals, List<Ticket> tickets, Instant now) {
        if (tickets.isEmpty()) {
            return;
        }

        Segment last = segment(tickets.getFirst());
        Segment returnSegment = last.reversed();
        nextTripDate(returnSegment, now).ifPresent(date -> addActionSignal(
                signals,
                SignalType.RECENT_RETURN,
                Map.of(
                        "origin", returnSegment.origin(),
                        "destination", returnSegment.destination(),
                        "recommendationKind", "return trip",
                        "originalTripOrigin", last.origin(),
                        "originalTripDestination", last.destination(),
                        "source", "most recent paid ticket; recommended direction is reversed for a return trip",
                        "suggestedDate", date.toString(),
                        "dateSource", "nearest upcoming matching trip"
                ),
                returnSegment,
                date
        ));

        tickets.stream()
                .collect(Collectors.groupingBy(this::segment, LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() >= 2)
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> {
                    Segment segment = entry.getKey();
                    nextTripDate(segment, now).ifPresent(date -> addActionSignal(
                            signals,
                            SignalType.FREQUENT_DIRECTION,
                            Map.of(
                                    "origin", segment.origin(),
                                    "destination", segment.destination(),
                                    "recommendationKind", "repeat frequent direction",
                                    "source", "client ticket history; this direction appears more than once",
                                    "suggestedDate", date.toString(),
                                    "dateSource", "nearest upcoming matching trip"
                            ),
                            segment,
                            date
                    ));
                });
    }

    private void addDemandSignals(List<RecommendationSignal> signals, List<TripRepository.TripOccupancyRow> rows) {
        rows.stream()
                .limit(8)
                .forEach(row -> tripRepository.findById(row.getTripId())
                        .map(this::routeSegment)
                        .ifPresent(segment -> {
                            LocalDate date = row.getDepartureAtUtc().atZone(ZoneId.systemDefault()).toLocalDate();
                            addActionSignal(
                                    signals,
                                    SignalType.HIGH_DEMAND,
                                    Map.of(
                                            "origin", segment.origin(),
                                            "destination", segment.destination(),
                                            "recommendationKind", "high demand",
                                            "travelDate", date.toString(),
                                            "reason", "elevated upcoming demand for this direction and date",
                                            "dateSource", "upcoming high-demand departure"
                                    ),
                                    segment,
                                    date
                            );
                        }));
    }

    private void addPopularSegmentSignals(
            List<RecommendationSignal> signals,
            List<TicketRepository.PopularSegmentRow> rows,
            Instant now
    ) {
        rows.stream()
                .limit(8)
                .forEach(row -> {
                    Segment segment = segment(row);
                    nextTripDate(segment, now).ifPresent(date -> addActionSignal(
                            signals,
                            SignalType.POPULAR_DIRECTION,
                            Map.of(
                                    "origin", segment.origin(),
                                    "destination", segment.destination(),
                                    "recommendationKind", "popular direction",
                                    "reason", "noticeable recent passenger interest for this direction",
                                    "suggestedDate", date.toString(),
                                    "dateSource", "nearest upcoming matching trip"
                            ),
                            segment,
                            date
                    ));
                });
    }

    private void addSeasonSignals(
            List<RecommendationSignal> signals,
            List<TicketRepository.PopularSegmentRow> rows,
            Instant now
    ) {
        String season = seasonName(today(now).getMonth());
        if (rows.isEmpty()) {
            return;
        }

        TicketRepository.PopularSegmentRow row = rows.getFirst();
        Segment segment = segment(row);
        nextTripDate(segment, now).ifPresent(date -> addActionSignal(
                signals,
                SignalType.SEASONAL_DIRECTION,
                Map.of(
                        "season", season,
                        "origin", segment.origin(),
                        "destination", segment.destination(),
                        "recommendationKind", "seasonal plus demand",
                        "suggestedDate", date.toString(),
                        "dateSource", "nearest upcoming matching trip",
                        "reason", "seasonal context plus recent passenger interest for this direction"
                ),
                segment,
                date
        ));
    }

    private void addCalendarSignals(
            List<RecommendationSignal> signals,
            List<TicketRepository.PopularSegmentRow> rows,
            Instant now
    ) {
        LocalDate today = today(now);
        rows.stream()
                .findFirst()
                .map(this::segment)
                .ifPresent(segment -> nextTripDate(segment, now).ifPresent(date -> addActionSignal(
                        signals,
                        SignalType.CALENDAR_EVENT,
                        calendarFacts(segment, today, date),
                        segment,
                        date
                )));

        if (today.getDayOfWeek().getValue() >= 4) {
            signals.add(new RecommendationSignal(
                    null,
                    SignalType.CALENDAR_EVENT,
                    Map.of(
                            "currentDate", today.toString(),
                            "weekday", today.getDayOfWeek().name(),
                            "event", "weekend is close",
                            "meaning", "passengers may prefer planning return or leisure trips in advance"
                    ),
                    null,
                    Set.of(),
                    Set.of()
            ));
        }
    }

    private List<RecommendationSignal> candidateSignals(List<List<RecommendationSignal>> buckets, Instant now) {
        List<RecommendationSignal> candidates = buckets.stream()
                .flatMap(List::stream)
                .toList();
        if (candidates.isEmpty()) {
            return List.of(genericSignal(now));
        }
        return candidates;
    }

    private List<RecommendationSignal> assignCandidateIds(List<RecommendationSignal> signals) {
        List<RecommendationSignal> shuffled = new ArrayList<>(signals);
        Collections.shuffle(shuffled, ThreadLocalRandom.current());

        List<RecommendationSignal> assigned = new ArrayList<>();
        int actionIndex = 1;
        int genericIndex = 1;
        for (RecommendationSignal signal : shuffled.stream().limit(MAX_CANDIDATES).toList()) {
            if (signal.action() == null) {
                assigned.add(signal.withId("G" + genericIndex++));
            } else {
                String id = "A" + actionIndex++;
                assigned.add(signal.withIdAndActionId(id));
            }
        }
        return List.copyOf(assigned);
    }

    private RecommendationSignal genericSignal(Instant now) {
        return new RecommendationSignal(
                null,
                SignalType.GENERIC,
                Map.of(
                        "currentDate", today(now).toString(),
                        "weekday", today(now).getDayOfWeek().name(),
                        "timeOfDay", timeOfDay(now)
                ),
                null,
                Set.of(),
                Set.of()
        );
    }

    private void addActionSignal(
            List<RecommendationSignal> signals,
            SignalType type,
            Map<String, String> facts,
            Segment segment,
            LocalDate date
    ) {
        Set<String> stations = new LinkedHashSet<>();
        stations.add(segment.origin());
        stations.add(segment.destination());
        signals.add(new RecommendationSignal(
                null,
                type,
                enrichFacts(facts, date),
                action(segment, date),
                stations,
                Set.of(date.toString())
        ));
    }

    private Map<String, String> enrichFacts(Map<String, String> facts, LocalDate travelDate) {
        Map<String, String> enriched = new LinkedHashMap<>(facts);
        LocalDate today = today(Instant.now());
        enriched.putIfAbsent("travelDate", travelDate.toString());
        enriched.put("travelDateRelation", dateRelation(travelDate, today));
        return Map.copyOf(enriched);
    }

    private String prompt(List<RecommendationSignal> candidates, String language, Instant now) {
        List<Map<String, Object>> modelCandidates = candidates.stream()
                .map(this::candidateForModel)
                .toList();

        return """
                Choose and generate one short passenger recommendation for the trip search page.

                Interface language: %s
                Current backend time UTC: %s
                Candidate recommendation signals:
                %s

                Return strict JSON only:
                {"message":"one useful sentence in the interface language","candidateId":"id of the chosen candidate"}

                Rules:
                - Choose exactly one candidate from the candidate list.
                - candidateId must be exactly one candidateId from the candidate list.
                - Prefer the candidate that is most useful for this passenger now: recent return trip, frequent direction, seasonal context, elevated upcoming demand, then general popularity.
                - Use only facts from the chosen candidate. Do not combine facts from different candidates.
                - Do not use outside knowledge.
                - Do not invent stations, dates, routes, prices, holidays, discounts, or demand.
                - Keep station names exactly as shown in the chosen candidate facts.
                - If facts contain origin and destination, mention both exact station names in the message.
                - Do not reveal ticket counts, seat counts, sold seats, total seats, trip ids, percentages, or phrases like "last 90 days".
                - Do not say that seats are almost gone, few seats remain, or seats are still available. The recommendation block does not show live inventory.
                - For HIGH_DEMAND, POPULAR_DIRECTION, SEASONAL_DIRECTION, and CALENDAR_EVENT with stations, write about the direction and date, not about one specific train.
                - Include a passenger-friendly reason from the chosen candidate: ticket history, recent passenger interest, elevated upcoming demand, calendar event, or season.
                - For RECENT_RETURN, describe it as a return trip after the original trip. Never say "repeat the recent trip" because the recommended direction is reversed.
                - For FREQUENT_DIRECTION, say the client often travels this exact direction.
                - For POPULAR_DIRECTION, say there is noticeable passenger interest in this direction, without counts.
                - For HIGH_DEMAND, say demand is elevated for this direction/date and suggest checking matching trips, without implying one specific train.
                - For SEASONAL_DIRECTION and CALENDAR_EVENT with station names, explain the season/calendar reason without counts.
                - If travelDateRelation is today, do not say "in advance", "ahead", or "plan ahead". Say to check today's availability instead.
                - Write for a passenger, not for an administrator.
                - Use a natural call to action. Keep it concise: 12-30 words.
                """.formatted(languageName(language), now, modelCandidates);
    }

    private Map<String, Object> candidateForModel(RecommendationSignal signal) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("candidateId", signal.id());
        candidate.put("type", signal.type().name());
        candidate.put("facts", publicFacts(signal));
        candidate.put("hasAction", signal.action() != null);
        return Map.copyOf(candidate);
    }

    private Map<String, String> publicFacts(RecommendationSignal signal) {
        Map<String, String> facts = new LinkedHashMap<>(signal.facts());
        if (signal.type() == SignalType.HIGH_DEMAND) {
            facts.put("subject", "direction and date, not one specific train");
            facts.put("reason", "elevated upcoming demand for this direction and date");
        } else if (signal.type() == SignalType.POPULAR_DIRECTION) {
            facts.put("subject", "direction");
            facts.put("reason", "noticeable recent passenger interest for this direction");
        } else if (signal.type() == SignalType.SEASONAL_DIRECTION) {
            facts.put("subject", "direction");
            facts.put("reason", "seasonal context plus recent passenger interest for this direction");
        } else if (signal.type() == SignalType.CALENDAR_EVENT) {
            facts.put("subject", signal.action() == null ? "general planning" : "direction");
        }
        facts.put("privacy", "only public wording is allowed; do not expose internal counts or inventory");
        return Map.copyOf(facts);
    }

    private ClientRecommendationResponse fallback(RecommendationSignal signal, String language) {
        if (signal.action() == null) {
            return ClientRecommendationResponse.localizedFallback(
                    fallbackMessage(language),
                    "client.recommendation.generic",
                    Map.of()
            );
        }

        String origin = signal.action().params().get("originStationName");
        String destination = signal.action().params().get("destinationStationName");
        String date = signal.action().params().get("travelDate");
        String message = isUkrainian(language)
                ? "Перевірте доступні рейси для %s - %s на %s.".formatted(origin, destination, date)
                : "Check available trips from %s to %s on %s.".formatted(origin, destination, date);
        return ClientRecommendationResponse.localized(
                message,
                "client.recommendation.search",
                Map.of("origin", origin, "destination", destination, "date", date),
                ACTION_TYPE,
                signal.action().params()
        );
    }

    private Optional<LocalDate> nextTripDate(Segment segment, Instant now) {
        return tripRepository.findTripsContainingSegment(segment.originStationId(), segment.destinationStationId())
                .stream()
                .map(trip -> departureForSegment(trip, segment.originStationId()))
                .flatMap(Optional::stream)
                .filter(departure -> departure.isAfter(now))
                .min(Comparator.naturalOrder())
                .map(departure -> departure.atZone(ZoneId.systemDefault()).toLocalDate());
    }

    private Optional<Instant> departureForSegment(Trip trip, Long originStationId) {
        return trip.getTripStops().stream()
                .filter(stop -> stop.getRouteStop().getStation().getId().equals(originStationId))
                .findFirst()
                .map(stop -> trip.getDepartureAtUtc().plus(stop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES));
    }

    private Segment routeSegment(Trip trip) {
        List<RouteStop> stops = trip.getRoute().getRouteStops().stream()
                .sorted(Comparator.comparing(RouteStop::getOrdinal))
                .toList();
        RouteStop first = stops.getFirst();
        RouteStop last = stops.getLast();
        return new Segment(
                first.getStation().getId(),
                first.getStation().getName(),
                last.getStation().getId(),
                last.getStation().getName()
        );
    }

    private Segment segment(Ticket ticket) {
        Station origin = ticket.getStartTripStop().getRouteStop().getStation();
        Station destination = ticket.getEndTripStop().getRouteStop().getStation();
        return new Segment(origin.getId(), origin.getName(), destination.getId(), destination.getName());
    }

    private Segment segment(TicketRepository.PopularSegmentRow row) {
        return new Segment(
                row.getStartStationId(),
                row.getStartStationName(),
                row.getEndStationId(),
                row.getEndStationName()
        );
    }

    private ActionCandidate action(Segment segment, LocalDate travelDate) {
        return new ActionCandidate(
                null,
                ACTION_TYPE,
                Map.of(
                        "originStationId", String.valueOf(segment.originStationId()),
                        "originStationName", segment.origin(),
                        "destinationStationId", String.valueOf(segment.destinationStationId()),
                        "destinationStationName", segment.destination(),
                        "travelDate", travelDate.toString()
                )
        );
    }

    private String seasonName(Month month) {
        if (month == Month.DECEMBER || month == Month.JANUARY || month == Month.FEBRUARY) {
            return "winter";
        }
        if (month == Month.MARCH || month == Month.APRIL || month == Month.MAY) {
            return "spring";
        }
        if (month == Month.JUNE || month == Month.JULY || month == Month.AUGUST) {
            return "summer";
        }
        return "autumn";
    }

    private Map<String, String> calendarFacts(Segment segment, LocalDate today, LocalDate travelDate) {
        if (today.getMonth() == Month.DECEMBER || today.getMonth() == Month.JANUARY) {
            return Map.of(
                    "event", "winter holiday period",
                    "origin", segment.origin(),
                    "destination", segment.destination(),
                    "currentDate", today.toString(),
                    "suggestedDate", travelDate.toString(),
                    "dateSource", "nearest upcoming matching trip",
                    "meaning", "early booking helps keep more available seat choices"
            );
        }
        if (today.getMonth() == Month.JUNE || today.getMonth() == Month.JULY || today.getMonth() == Month.AUGUST) {
            return Map.of(
                    "event", "summer travel season",
                    "origin", segment.origin(),
                    "destination", segment.destination(),
                    "currentDate", today.toString(),
                    "suggestedDate", travelDate.toString(),
                    "dateSource", "nearest upcoming matching trip",
                    "meaning", "seasonal demand can make convenient departure times sell faster"
            );
        }
        return Map.of(
                "event", "calendar planning",
                "origin", segment.origin(),
                "destination", segment.destination(),
                "currentDate", today.toString(),
                "suggestedDate", travelDate.toString(),
                "dateSource", "nearest upcoming matching trip",
                "meaning", "planning ahead improves chances of finding a convenient trip"
        );
    }

    private String timeOfDay(Instant now) {
        int hour = now.atZone(ZoneId.systemDefault()).getHour();
        if (hour < 6) {
            return "night";
        }
        if (hour < 12) {
            return "morning";
        }
        if (hour < 18) {
            return "afternoon";
        }
        return "evening";
    }

    private String languageName(String language) {
        if (isUkrainian(language)) {
            return "Ukrainian";
        }
        String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("en")) {
            return "English";
        }
        return StringUtils.hasText(language) ? language.trim() : "the interface language";
    }

    private boolean isUkrainian(String language) {
        String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("ua") || normalized.equals("uk-ua") || normalized.startsWith("uk");
    }

    private String fallbackMessage(String language) {
        return isUkrainian(language) ? FALLBACK_MESSAGE_UK : FALLBACK_MESSAGE_EN;
    }

    private String dateRelation(LocalDate date, LocalDate today) {
        if (date.equals(today)) {
            return "today";
        }
        if (date.equals(today.plusDays(1))) {
            return "tomorrow";
        }
        return date.isAfter(today) ? "future" : "past";
    }

    private LocalDate today(Instant now) {
        return LocalDate.ofInstant(now, ZoneId.systemDefault());
    }

    public record RecommendationContext(
            String prompt,
            List<RecommendationSignal> candidates,
            ClientRecommendationResponse fallbackResponse
    ) {
        public Optional<ActionCandidate> actionByCandidateId(String candidateId) {
            if (!StringUtils.hasText(candidateId)) {
                return Optional.empty();
            }
            return candidates.stream()
                    .filter(candidate -> candidate.id().equalsIgnoreCase(candidateId.trim()))
                    .map(RecommendationSignal::action)
                    .filter(Objects::nonNull)
                    .findFirst();
        }

        public boolean isExpectedCandidate(String candidateId) {
            if (!StringUtils.hasText(candidateId)) {
                return false;
            }
            return candidates.stream().anyMatch(candidate -> candidate.id().equalsIgnoreCase(candidateId.trim()));
        }

        public boolean isValidMessage(String message) {
            if (!StringUtils.hasText(message) || message.length() > MAX_MESSAGE_LENGTH) {
                return false;
            }
            String trimmed = message.trim();
            if (trimmed.contains("{") || trimmed.contains("}") || trimmed.contains("\"message")) {
                return false;
            }
            long questionMarks = trimmed.chars().filter(character -> character == '?').count();
            return !trimmed.contains("???") && questionMarks <= Math.max(3, trimmed.length() / 5);
        }
    }

    public record RecommendationSignal(
            String id,
            SignalType type,
            Map<String, String> facts,
            ActionCandidate action,
            Set<String> allowedStationNames,
            Set<String> allowedDates
    ) {
        private RecommendationSignal withId(String id) {
            return new RecommendationSignal(id, type, facts, action, allowedStationNames, allowedDates);
        }

        private RecommendationSignal withIdAndActionId(String id) {
            return new RecommendationSignal(id, type, facts, action.withId(id), allowedStationNames, allowedDates);
        }
    }

    public record ActionCandidate(String id, String type, Map<String, String> params) {
        private ActionCandidate withId(String id) {
            return new ActionCandidate(id, type, params);
        }
    }

    public enum SignalType {
        RECENT_RETURN,
        FREQUENT_DIRECTION,
        HIGH_DEMAND,
        POPULAR_DIRECTION,
        SEASONAL_DIRECTION,
        CALENDAR_EVENT,
        GENERIC
    }

    private record Segment(Long originStationId, String origin, Long destinationStationId, String destination) {
        private Segment reversed() {
            return new Segment(destinationStationId, destination, originStationId, origin);
        }
    }
}
