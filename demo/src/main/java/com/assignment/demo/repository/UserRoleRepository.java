package com.assignment.demo.repository;

import com.assignment.demo.entity.UserRole;
import com.assignment.demo.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    Optional<UserRole> findByName(Role name);
}
