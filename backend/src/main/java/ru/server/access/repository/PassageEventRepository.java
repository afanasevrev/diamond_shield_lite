package ru.server.access.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.server.access.entity.PassageEvent;

public interface PassageEventRepository extends JpaRepository<PassageEvent, Long> {
    @EntityGraph(attributePaths = {"person", "controller"})
    Page<PassageEvent> findAllByOrderByEventTimeDesc(Pageable pageable);
}