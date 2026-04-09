package com.datong.mathai.auth;

import com.datong.mathai.common.AppException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    private final RowMapper<UserRow> userRowMapper = (rs, rowNum) -> new UserRow(
        rs.getLong("id"),
        rs.getString("username"),
        rs.getString("display_name"),
        rs.getString("password_hash")
    );

    public AuthService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM users WHERE username = ?",
            Integer.class,
            request.username()
        );
        if (count != null && count > 0) {
            throw new AppException(409, "Username already exists");
        }

        String displayName = request.username();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users(username, password_hash, display_name) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, request.username());
            ps.setString(2, passwordEncoder.encode(request.password()));
            ps.setString(3, displayName);
            return ps;
        }, keyHolder);

        Long userId = extractId(keyHolder, "Create user failed");
        String token = createToken(userId);
        return new AuthResponse(token, new UserProfile(userId, request.username(), displayName));
    }

    public AuthResponse login(LoginRequest request) {
        var users = jdbcTemplate.query(
            "SELECT id, username, display_name, password_hash FROM users WHERE username = ?",
            userRowMapper,
            request.username()
        );
        if (users.isEmpty()) {
            throw new AppException(401, "Invalid username or password");
        }

        UserRow user = users.get(0);
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new AppException(401, "Invalid username or password");
        }

        String token = createToken(user.id());
        return new AuthResponse(token, new UserProfile(user.id(), user.username(), user.displayName()));
    }

    public Long requireUserId(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new AppException(401, "Not logged in");
        }
        String token = authorizationHeader.replace("Bearer", "").trim();
        Long userId = sessions.get(token);
        if (userId == null) {
            throw new AppException(401, "Session expired");
        }
        return userId;
    }

    private String createToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, userId);
        return token;
    }

    private Long extractId(KeyHolder keyHolder, String errorMessage) {
        try {
            Number key = keyHolder.getKey();
            if (key != null) {
                return key.longValue();
            }
        } catch (Exception ignored) {
        }
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys != null) {
            Object idValue = keys.get("id");
            if (idValue instanceof Number number) {
                return number.longValue();
            }
            Object firstValue = keys.values().stream().findFirst().orElse(null);
            if (firstValue instanceof Number number) {
                return number.longValue();
            }
        }
        throw new AppException(500, errorMessage);
    }

    private record UserRow(Long id, String username, String displayName, String passwordHash) {
    }
}
