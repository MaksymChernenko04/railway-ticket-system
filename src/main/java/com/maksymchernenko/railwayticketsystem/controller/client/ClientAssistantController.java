package com.maksymchernenko.railwayticketsystem.controller.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientAssistantChatRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientAssistantChatResponse;
import com.maksymchernenko.railwayticketsystem.service.client.ClientAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/assistant")
@RequiredArgsConstructor
public class ClientAssistantController {

    private final ClientAssistantService clientAssistantService;

    @PostMapping("/chat")
    public ClientAssistantChatResponse chat(@Valid @RequestBody ClientAssistantChatRequest request) {
        return clientAssistantService.chat(request);
    }
}
