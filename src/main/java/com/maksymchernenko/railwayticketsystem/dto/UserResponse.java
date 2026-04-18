package com.maksymchernenko.railwayticketsystem.dto;

public record UserResponse(
        Long id,
        String email,
        String role,
        String firstName,
        String lastName
) {
}
