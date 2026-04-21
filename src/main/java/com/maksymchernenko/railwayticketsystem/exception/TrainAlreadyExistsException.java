package com.maksymchernenko.railwayticketsystem.exception;

public class TrainAlreadyExistsException extends RuntimeException {
    public TrainAlreadyExistsException(String message) {
        super(message);
    }
}
