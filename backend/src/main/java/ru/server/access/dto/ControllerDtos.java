package ru.server.access.dto;

import jakarta.validation.constraints.*;

public final class ControllerDtos {

    private ControllerDtos() {
    }

    public record CreateControllerRequest(
            @NotBlank String name,
            @NotBlank String ip,
            String webSocketUrl,
            String password
    ) {
    }

    public record ControllerResponse(
            Long id,
            String name,
            String ip,
            String webSocketUrl,
            boolean enabled,
            boolean connected,
            boolean authenticated,
            String lastSeen
    ) {
    }

    public record CreateReaderRequest(
            @Min(0) int number,
            @NotBlank String name,
            @NotBlank String type,
            @Min(0) int port,
            @Min(0) @Max(1) int exdevNumber,
            @Min(0) @Max(1) int exdevDirection
    ) {
    }

    public record ReaderResponse(
            Long id,
            int number,
            String name,
            String type,
            int port,
            int exdevNumber,
            int exdevDirection
    ) {
    }

    public record CommandResponse(
            boolean sent,
            String json
    ) {
    }
}