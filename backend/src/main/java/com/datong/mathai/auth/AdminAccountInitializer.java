package com.datong.mathai.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "123456";
    private static final String ADMIN_DISPLAY_NAME = "Admin";
    private static final String ADMIN_ROLE = "ADMIN";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String passwordHash = passwordEncoder.encode(ADMIN_PASSWORD);
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM users WHERE username = ?",
            Integer.class,
            ADMIN_USERNAME
        );

        if (count != null && count > 0) {
            jdbcTemplate.update(
                "UPDATE users SET password_hash = ?, display_name = ?, role = ? WHERE username = ?",
                passwordHash,
                ADMIN_DISPLAY_NAME,
                ADMIN_ROLE,
                ADMIN_USERNAME
            );
            return;
        }

        jdbcTemplate.update(
            "INSERT INTO users(username, password_hash, display_name, role) VALUES(?,?,?,?)",
            ADMIN_USERNAME,
            passwordHash,
            ADMIN_DISPLAY_NAME,
            ADMIN_ROLE
        );
    }
}
