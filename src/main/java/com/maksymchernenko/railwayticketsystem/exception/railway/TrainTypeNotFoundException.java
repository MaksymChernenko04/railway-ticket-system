package com.maksymchernenko.railwayticketsystem.exception.railway;

public class TrainTypeNotFoundException extends RuntimeException {
    public TrainTypeNotFoundException(String message) {
        super(message);
    }
}
