package com.datong.mathai.mistake;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMistakeRequest(
    Long chapterId,
    @NotBlank(message = "Question title is required")
    @Size(max = 120, message = "Question title must be <= 120 characters")
    String questionTitle,
    @NotBlank(message = "Question content is required")
    @Size(max = 4000, message = "Question content must be <= 4000 characters")
    String questionContent,
    @Size(max = 1024, message = "Image URL must be <= 1024 characters")
    String imageUrl
) {
}