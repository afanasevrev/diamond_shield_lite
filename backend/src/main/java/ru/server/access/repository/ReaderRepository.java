package ru.server.access.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.server.access.entity.Reader;

import java.util.List;

public interface ReaderRepository extends JpaRepository<Reader, Long> {
    List<Reader> findAllByControllerIdOrderByNumber(Long controllerId);
    boolean existsByControllerIdAndNumber(Long controllerId, int number);
}