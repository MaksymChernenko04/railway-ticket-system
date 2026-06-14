package com.maksymchernenko.railwayticketsystem.exception.client;

public class TripClientValidationException extends RuntimeException {

    public TripClientValidationException(String message) {
        super(message);
    }
}
