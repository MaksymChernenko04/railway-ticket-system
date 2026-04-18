package com.maksymchernenko.railwayticketsystem.exception.auth;

public class RoleNotConfiguredException extends RuntimeException {
    public RoleNotConfiguredException(String message) {
        super(message);
    }
}
