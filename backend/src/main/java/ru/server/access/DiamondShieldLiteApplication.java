package ru.server.access;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.server.access.service.AdminService;

@SpringBootApplication
@EnableScheduling
public class DiamondShieldLiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiamondShieldLiteApplication.class, args);
	}
	@Bean
	public CommandLineRunner createDefaultAdmin(AdminService adminService) {
		return args -> adminService.createDefaultAdminIfRequired();
	}
}