package ru.server.access.controller;


import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.server.access.dto.PersonDtos.PersonResponse;
import ru.server.access.entity.Person;
import ru.server.access.service.PersonService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    public List<PersonResponse> findAll() {
        return personService.findAll();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PersonResponse create(
            @RequestParam String lastName,
            @RequestParam String firstName,
            @RequestParam(required = false) String middleName,
            @RequestParam String cardId,
            @RequestPart(required = false) MultipartFile photo
    ) throws IOException {
        return personService.create(
                lastName,
                firstName,
                middleName,
                cardId,
                photo
        );
    }

    @PatchMapping("/{id}/active")
    public PersonResponse setActive(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        return personService.setActive(id, active);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        personService.delete(id);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) {
        Person person = personService.findEntity(id);

        if (person.getPhoto() == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;

        if (person.getPhotoContentType() != null) {
            contentType = MediaType.parseMediaType(
                    person.getPhotoContentType()
            );
        }

        return ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.noStore())
                .body(person.getPhoto());
    }
}