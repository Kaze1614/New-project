package com.datong.mathai.admin;

import java.time.LocalDateTime;

public record AdminUserItem(
    Long id,
    String username,
    LocalDateTime createdAt,
    String role
) {
}
