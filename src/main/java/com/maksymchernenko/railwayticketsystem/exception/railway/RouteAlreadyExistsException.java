package com.maksymchernenko.railwayticketsystem.exception.railway;

public class RouteAlreadyExistsException extends RuntimeException {
    public RouteAlreadyExistsException(String message) {
        super(message);
    }
}
