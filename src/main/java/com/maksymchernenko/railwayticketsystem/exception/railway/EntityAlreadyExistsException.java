package com.maksymchernenko.railwayticketsystem.exception.railway;

public class EntityAlreadyExistsException extends RuntimeException {
    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}
