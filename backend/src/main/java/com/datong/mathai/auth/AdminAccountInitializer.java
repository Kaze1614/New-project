package com.datong.mathai.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "123456";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM users WHERE username = ?",
            Integer.class,
            ADMIN_USERNAME
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
            "INSERT INTO users(username, password_hash, display_name) VALUES(?,?,?)",
            ADMIN_USERNAME,
            passwordEncoder.encode(ADMIN_PASSWORD),
            "admin"
        );
        log.info("Initialized default admin account: username={}", ADMIN_USERNAME);
    }
}
