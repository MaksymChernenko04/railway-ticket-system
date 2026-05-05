package com.maksymchernenko.railwayticketsystem.exception.railway;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}
