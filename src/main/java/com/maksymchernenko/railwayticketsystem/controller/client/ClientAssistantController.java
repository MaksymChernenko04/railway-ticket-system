package com.maksymchernenko.railwayticketsystem.controller.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientAssistantChatRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientAssistantChatResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientRecommendationResponse;
import com.maksymchernenko.railwayticketsystem.security.CustomUserDetails;
import com.maksymchernenko.railwayticketsystem.service.client.ClientAssistantService;
import com.maksymchernenko.railwayticketsystem.service.client.ClientRecommendationAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/assistant")
@RequiredArgsConstructor
public class ClientAssistantController {

    private final ClientAssistantService clientAssistantService;
    private final ClientRecommendationAiService clientRecommendationAiService;

    @PostMapping("/chat")
    public ClientAssistantChatResponse chat(@Valid @RequestBody ClientAssistantChatRequest request) {
        return clientAssistantService.chat(request);
    }

    @GetMapping("/search-recommendation")
    public ClientRecommendationResponse searchRecommendation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "en") String language
    ) {
        return clientRecommendationAiService.generate(userDetails.getId(), language);
    }
}
