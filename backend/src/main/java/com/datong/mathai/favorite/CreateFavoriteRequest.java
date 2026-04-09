package com.datong.mathai.favorite;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFavoriteRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must be <= 120 characters")
    String title,
    @NotBlank(message = "Content is required")
    @Size(max = 4000, message = "Content must be <= 4000 characters")
    String content
) {
}