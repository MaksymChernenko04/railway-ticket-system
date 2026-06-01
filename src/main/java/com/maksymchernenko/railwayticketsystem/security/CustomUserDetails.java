package com.maksymchernenko.railwayticketsystem.security;

import com.maksymchernenko.railwayticketsystem.model.auth.User;
import jakarta.annotation.Nonnull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(User user) implements UserDetails {

    public Long getId() {
        return user.getId();
    }

    public String getRole() {
        return user.getRole().getName();
    }

    @Override
    @Nonnull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + getRole()));
    }

    @Override
    public @Nullable String getPassword() {
        String passwordHash = user.getPasswordHash();
        return passwordHash != null ? passwordHash.trim() : null;
    }

    @Override
    @Nonnull
    public String getUsername() {
        return user.getEmail();
    }
}
