package com.maksymchernenko.railwayticketsystem.service.client;

public interface TicketLifecycleService {

    void finishCompletedActiveTicketsForUser(Long userId);
}
