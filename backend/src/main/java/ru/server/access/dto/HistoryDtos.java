package ru.server.access.dto;


public final class HistoryDtos {

    private HistoryDtos() {
    }

    public record HistoryResponse(
            Long id,
            String eventType,
            String cardId,
            String fullName,
            String controllerName,
            Integer deviceNumber,
            Integer direction,
            boolean allowed,
            Boolean removeCard,
            String commandSource,
            String eventTime
    ) {
    }
}