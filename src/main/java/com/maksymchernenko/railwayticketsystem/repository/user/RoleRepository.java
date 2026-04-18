package com.maksymchernenko.railwayticketsystem.repository.user;

import com.maksymchernenko.railwayticketsystem.model.reference.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, String> {
}
