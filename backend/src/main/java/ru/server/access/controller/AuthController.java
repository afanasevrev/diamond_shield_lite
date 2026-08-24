package ru.server.access.controller;


import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.server.access.dto.AdminDtos.CurrentUserResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public CurrentUserResponse currentUser(
            Authentication authentication
    ) {
        return new CurrentUserResponse(authentication.getName());
    }
}