package com.maksymchernenko.railwayticketsystem.controller.admin;

import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAnalyticsRequest;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAnalyticsResponse;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAssistantChatRequest;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAssistantChatResponse;
import com.maksymchernenko.railwayticketsystem.service.admin.AdminAnalyticsService;
import com.maksymchernenko.railwayticketsystem.service.admin.AdminAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final AdminAssistantService adminAssistantService;

    @GetMapping
    public AdminAnalyticsResponse getAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @RequestParam(required = false) Long routeId,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return adminAnalyticsService.getAnalytics(new AdminAnalyticsRequest(dateFrom, dateTo, routeId, limit));
    }

    @PostMapping("/chat")
    public AdminAssistantChatResponse chat(@Valid @RequestBody AdminAssistantChatRequest request) {
        return adminAssistantService.chat(request);
    }
}
