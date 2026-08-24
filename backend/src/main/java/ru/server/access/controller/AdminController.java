package ru.server.access.controller;


import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.server.access.dto.AdminDtos.*;
import ru.server.access.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<AdminResponse> findAll() {
        return adminService.findAll();
    }

    @PostMapping
    public AdminResponse create(
            @Valid @RequestBody CreateAdminRequest request
    ) {
        return adminService.create(request);
    }
}