package com.maksymchernenko.railwayticketsystem.service.ai;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatHistoryService {

    private static final int MAX_HISTORY_TURNS = 6;

    private final Map<String, Deque<ChatTurn>> conversationHistory = new ConcurrentHashMap<>();

    public String format(String conversationId, String userRoleName) {
        Deque<ChatTurn> history = conversationHistory.get(conversationId);
        if (history == null || history.isEmpty()) {
            return "No previous messages.";
        }

        StringBuilder builder = new StringBuilder();
        synchronized (history) {
            for (ChatTurn turn : history) {
                builder.append(userRoleName).append(": ").append(turn.userMessage()).append('\n');
                builder.append("Assistant: ").append(turn.assistantAnswer()).append('\n');
            }
        }
        return builder.toString();
    }

    public void remember(String conversationId, String userMessage, String assistantAnswer) {
        Deque<ChatTurn> history = conversationHistory.computeIfAbsent(conversationId, ignored -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(new ChatTurn(userMessage, assistantAnswer));
            while (history.size() > MAX_HISTORY_TURNS) {
                history.removeFirst();
            }
        }
    }

    private record ChatTurn(String userMessage, String assistantAnswer) {
    }
}
