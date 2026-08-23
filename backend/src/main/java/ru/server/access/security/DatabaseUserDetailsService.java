package ru.server.access.security;


import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import ru.server.access.entity.Admin;
import ru.server.access.repository.AdminRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;

    public DatabaseUserDetailsService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        Admin admin = adminRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Администратор не найден")
                );

        return User.withUsername(admin.getUsername())
                .password(admin.getPasswordHash())
                .roles("ADMIN")
                .disabled(!admin.isEnabled())
                .build();
    }
}