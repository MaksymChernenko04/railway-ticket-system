package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.model.booking.Order;

public interface OrderLifecycleService {

    void expireOverduePendingOrdersForUser(Long userId);

    void expireIfOverdue(Order order);
}
