package com.maksymchernenko.railwayticketsystem.exception;

public class TrainTypeNotFoundException extends RuntimeException {
    public TrainTypeNotFoundException(String message) {
        super(message);
    }
}
