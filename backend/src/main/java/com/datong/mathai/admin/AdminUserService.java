package com.datong.mathai.admin;

import com.datong.mathai.common.AppException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String RESET_PASSWORD = "123456";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<AdminUserItem> list(String keyword, Integer page, Integer size) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.min(Math.max(size == null ? 20 : size, 1), MAX_PAGE_SIZE);
        int offset = (safePage - 1) * safeSize;

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        ArrayList<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (username LIKE ? OR display_name LIKE ? OR role LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM users" + where, Long.class, args.toArray());
        ArrayList<Object> queryArgs = new ArrayList<>(args);
        queryArgs.add(safeSize);
        queryArgs.add(offset);

        var items = jdbcTemplate.query(
            """
                SELECT id, username, created_at, role
                FROM users
                """ + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
            (rs, rowNum) -> new AdminUserItem(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getString("role")
            ),
            queryArgs.toArray()
        );

        return new PageResult<>(items, safePage, safeSize, total == null ? 0 : total);
    }

    public void resetPassword(Long id) {
        int affected = jdbcTemplate.update(
            "UPDATE users SET password_hash = ? WHERE id = ?",
            passwordEncoder.encode(RESET_PASSWORD),
            id
        );
        if (affected == 0) {
            throw new AppException(404, "用户不存在");
        }
    }

    public void delete(Long currentUserId, Long id) {
        var users = jdbcTemplate.query(
            "SELECT id, username FROM users WHERE id = ?",
            (rs, rowNum) -> new UserRow(rs.getLong("id"), rs.getString("username")),
            id
        );
        if (users.isEmpty()) {
            throw new AppException(404, "用户不存在");
        }

        UserRow user = users.get(0);
        if ("admin".equalsIgnoreCase(user.username())) {
            throw new AppException(400, "admin 账号不可删除");
        }
        if (user.id().equals(currentUserId)) {
            throw new AppException(400, "当前登录账号不可删除");
        }

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    private record UserRow(Long id, String username) {
    }
}
