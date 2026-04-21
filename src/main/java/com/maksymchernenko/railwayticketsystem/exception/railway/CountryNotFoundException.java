package com.maksymchernenko.railwayticketsystem.exception.railway;

public class CountryNotFoundException extends RuntimeException {

    public CountryNotFoundException(String message) {
        super(message);
    }
}
