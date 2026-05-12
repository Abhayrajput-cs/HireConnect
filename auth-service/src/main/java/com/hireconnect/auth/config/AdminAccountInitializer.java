package com.hireconnect.auth.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.hireconnect.auth.domain.UserCredential;
import com.hireconnect.auth.repository.AuthRepository;

@Component
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminAccountInitializer implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(
        AdminBootstrapProperties properties,
        AuthRepository authRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled() || !StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
            return;
        }
        authRepository.findByEmail(properties.email().trim().toLowerCase())
            .orElseGet(() -> {
                UserCredential admin = new UserCredential();
                admin.setEmail(properties.email().trim().toLowerCase());
                admin.setFullName("HireConnect Admin");
                admin.setPasswordHash(passwordEncoder.encode(properties.password()));
                admin.setRole("ADMIN");
                admin.setProvider("LOCAL");
                admin.setEmailVerified(true);
                return authRepository.save(admin);
            });
    }
}
