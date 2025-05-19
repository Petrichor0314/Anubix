package com.petrichor.auth_service;

import com.petrichor.auth_service.model.User;
import com.petrichor.auth_service.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			User existingUser = userRepository.findByUsername("user").block();
			if (existingUser == null) {
				User user = new User(null, "user", passwordEncoder.encode("password"), true,
						Arrays.asList("ROLE_USER"));
				userRepository.save(user)
						.doOnSuccess(u -> System.out.println("Created default user: " + u.getUsername()))
						.subscribe();
			} else {
				System.out.println("Default user 'user' already exists.");
			}

			User existingAdmin = userRepository.findByUsername("admin").block();
			if (existingAdmin == null) {
				User admin = new User(null, "admin", passwordEncoder.encode("adminpass"), true,
						Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
				userRepository.save(admin)
						.doOnSuccess(u -> System.out.println("Created default admin: " + u.getUsername()))
						.subscribe();
			} else {
				System.out.println("Default user 'admin' already exists.");
			}
		};
	}
}
