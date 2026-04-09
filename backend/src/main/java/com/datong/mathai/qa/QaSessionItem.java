package com.datong.mathai.qa;

import java.time.LocalDateTime;

public record QaSessionItem(Long id, String title, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
