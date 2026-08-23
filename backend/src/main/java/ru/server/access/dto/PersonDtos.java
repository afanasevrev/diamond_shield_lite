package ru.server.access.dto;

public final class PersonDtos {

    private PersonDtos() {
    }

    public record PersonResponse(
            Long id,
            String lastName,
            String firstName,
            String middleName,
            String cardId,
            boolean active,
            String photoUrl
    ) {
    }
}
