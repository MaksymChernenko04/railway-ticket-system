package com.maksymchernenko.railwayticketsystem.exception.railway;

public class CurrencyNotFoundException extends RuntimeException {
    public CurrencyNotFoundException(String message) {
        super(message);
    }
}
