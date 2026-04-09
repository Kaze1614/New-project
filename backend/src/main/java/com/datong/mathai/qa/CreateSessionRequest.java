package com.datong.mathai.qa;

import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
    @Size(max = 80, message = "Session title must be <= 80 characters")
    String title
) {
}