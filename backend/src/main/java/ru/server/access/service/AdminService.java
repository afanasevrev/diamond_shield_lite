package ru.server.access.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.server.access.dto.AdminDtos.*;
import ru.server.access.entity.Admin;
import ru.server.access.exception.BadRequestException;
import ru.server.access.repository.AdminRepository;

import java.util.List;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultUsername;
    private final String defaultPassword;

    public AdminService(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.default-admin.username}") String defaultUsername,
            @Value("${app.default-admin.password}") String defaultPassword
    ) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultUsername = defaultUsername;
        this.defaultPassword = defaultPassword;
    }

    @Transactional
    public void createDefaultAdminIfRequired() {
        if (adminRepository.count() != 0) {
            return;
        }

        Admin admin = new Admin();
        admin.setUsername(defaultUsername);
        admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
        admin.setEnabled(true);

        adminRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> findAll() {
        return adminRepository.findAll().stream()
                .map(this::map)
                .toList();
    }

    @Transactional
    public AdminResponse create(CreateAdminRequest request) {
        String username = request.username().trim();

        if (adminRepository.existsByUsername(username)) {
            throw new BadRequestException(
                    "Администратор с таким логином уже существует"
            );
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        admin.setEnabled(true);

        return map(adminRepository.save(admin));
    }

    private AdminResponse map(Admin admin) {
        return new AdminResponse(
                admin.getId(),
                admin.getUsername(),
                admin.isEnabled(),
                admin.getCreatedAt().toString()
        );
    }
}