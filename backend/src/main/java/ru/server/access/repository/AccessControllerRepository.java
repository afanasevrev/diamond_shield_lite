package ru.server.access.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.server.access.entity.AccessController;

import java.util.List;
import java.util.Optional;

public interface AccessControllerRepository extends JpaRepository<AccessController, Long> {
    Optional<AccessController> findFirstByIp(String ip);
    boolean existsByIp(String ip);
    List<AccessController> findAllByEnabledTrue();
}