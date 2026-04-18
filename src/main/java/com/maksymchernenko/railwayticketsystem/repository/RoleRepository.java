package com.maksymchernenko.railwayticketsystem.repository;

import com.maksymchernenko.railwayticketsystem.model.reference.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}
