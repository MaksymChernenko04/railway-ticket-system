package com.maksymchernenko.railwayticketsystem.repository.user;

import com.maksymchernenko.railwayticketsystem.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
