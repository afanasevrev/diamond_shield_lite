package ru.server.access.dto;


public final class LiveDtos {

    private LiveDtos() {
    }

    public record LiveCardEvent(
            String eventType,
            String cardId,
            boolean allowed,
            Long personId,
            String fullName,
            String photoUrl,
            Long controllerId,
            String controllerName,
            Integer deviceNumber,
            Integer direction,
            String eventTime
    ) {
    }
}