package com.shoplab.auth.service;

import com.shoplab.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InitializerService implements ApplicationRunner {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${security.initial.admin-username:admin}")
    private String adminUsername;
    @Value("${security.initial.admin-password:Admin123!}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!users.existsByUsername(adminUsername)) {
            long id = users.insert(adminUsername, encoder.encode(adminPassword), true);
            users.assignRole(id, "ADMIN");
            users.assignRole(id, "USER");
        }
    }
}
