package ru.server.access.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.server.access.entity.Person;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByCardId(String cardId);
    boolean existsByCardId(String cardId);
}