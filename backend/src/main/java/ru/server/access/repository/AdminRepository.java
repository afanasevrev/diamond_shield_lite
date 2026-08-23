package ru.server.access.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.server.access.entity.Admin;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);
    boolean existsByUsername(String username);
}