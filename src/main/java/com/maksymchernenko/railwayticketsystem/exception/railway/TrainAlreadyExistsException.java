package com.maksymchernenko.railwayticketsystem.exception.railway;

public class TrainAlreadyExistsException extends RuntimeException {
    public TrainAlreadyExistsException(String message) {
        super(message);
    }
}
