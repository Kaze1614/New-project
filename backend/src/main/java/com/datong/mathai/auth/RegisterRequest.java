package com.datong.mathai.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 32, message = "Username length must be 3-32")
    String username,
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 64, message = "Password length must be 6-64")
    String password
) {
}
