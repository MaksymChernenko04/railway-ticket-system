package com.maksymchernenko.railwayticketsystem.repository.auth;

import com.maksymchernenko.railwayticketsystem.model.reference.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}
