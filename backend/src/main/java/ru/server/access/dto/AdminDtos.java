package ru.server.access.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record CreateAdminRequest(
            @NotBlank String username,
            @NotBlank @Size(min = 8, max = 100)
            String password)
    {}

    public record AdminResponse(Long id, String username, boolean enabled, String createdAt) {}

    public record CurrentUserResponse(String username) {}
}