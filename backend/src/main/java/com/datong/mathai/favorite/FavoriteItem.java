package com.datong.mathai.favorite;

import java.time.LocalDateTime;

public record FavoriteItem(Long id, String title, String content, LocalDateTime createdAt) {
}
