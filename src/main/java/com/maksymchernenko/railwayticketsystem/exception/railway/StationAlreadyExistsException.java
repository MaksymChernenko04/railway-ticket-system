package com.maksymchernenko.railwayticketsystem.exception.railway;

public class StationAlreadyExistsException extends RuntimeException {

    public StationAlreadyExistsException(String message) {
        super(message);
    }
}
