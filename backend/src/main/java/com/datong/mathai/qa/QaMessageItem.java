package com.datong.mathai.qa;

import java.time.LocalDateTime;

public record QaMessageItem(Long id, Long sessionId, String role, String content, LocalDateTime createdAt) {
}
