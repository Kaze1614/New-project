package com.datong.mathai.favorite;

import java.time.LocalDateTime;

public record FavoriteItem(
    Long id,
    Long chapterId,
    String difficulty,
    String title,
    String content,
    LocalDateTime createdAt
) {
}
