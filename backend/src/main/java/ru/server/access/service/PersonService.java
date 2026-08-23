package ru.server.access.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.server.access.dto.PersonDtos.PersonResponse;
import ru.server.access.entity.Person;
import ru.server.access.exception.*;
import ru.server.access.repository.PersonRepository;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class PersonService {

    public static final int MAX_PHOTO_SIZE = 100 * 1024;

    private static final Set<String> ALLOWED_PHOTO_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> findAll() {
        return personRepository.findAll().stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public PersonResponse create(
            String lastName,
            String firstName,
            String middleName,
            String cardId,
            MultipartFile photo
    ) throws IOException {

        String normalizedCardId = requireText(cardId, "Идентификатор карты");

        if (personRepository.existsByCardId(normalizedCardId)) {
            throw new BadRequestException(
                    "Человек с такой картой уже существует"
            );
        }

        Person person = new Person();
        person.setLastName(requireText(lastName, "Фамилия"));
        person.setFirstName(requireText(firstName, "Имя"));
        person.setMiddleName(normalizeNullable(middleName));
        person.setCardId(normalizedCardId);
        person.setActive(true);

        if (photo != null && !photo.isEmpty()) {
            validatePhoto(photo);
            person.setPhoto(photo.getBytes());
            person.setPhotoContentType(photo.getContentType());
        }

        return map(personRepository.save(person));
    }

    @Transactional
    public void delete(Long id) {
        Person person = findEntity(id);
        personRepository.delete(person);
    }

    @Transactional
    public PersonResponse setActive(Long id, boolean active) {
        Person person = findEntity(id);
        person.setActive(active);
        return map(person);
    }

    @Transactional(readOnly = true)
    public Person findEntity(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Человек не найден"));
    }

    private void validatePhoto(MultipartFile photo) {
        if (photo.getSize() > MAX_PHOTO_SIZE) {
            throw new BadRequestException(
                    "Фотография не должна превышать 100 КБ"
            );
        }

        if (photo.getContentType() == null
                || !ALLOWED_PHOTO_TYPES.contains(photo.getContentType())) {
            throw new BadRequestException(
                    "Допускаются фотографии JPEG, PNG и WebP"
            );
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(
                    "Поле \"" + fieldName + "\" обязательно"
            );
        }

        return value.trim();
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PersonResponse map(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getLastName(),
                person.getFirstName(),
                person.getMiddleName(),
                person.getCardId(),
                person.isActive(),
                person.getPhoto() == null
                        ? null
                        : "/api/persons/" + person.getId() + "/photo"
        );
    }
}