package com.maksymchernenko.railwayticketsystem.exception.station;

public class StationAlreadyExistsException extends RuntimeException {

    public StationAlreadyExistsException(String message) {
        super(message);
    }
}
