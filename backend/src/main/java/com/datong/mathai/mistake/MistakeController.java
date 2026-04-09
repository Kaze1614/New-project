package com.datong.mathai.mistake;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mistakes")
public class MistakeController {

    private final AuthService authService;
    private final MistakeService mistakeService;

    public MistakeController(AuthService authService, MistakeService mistakeService) {
        this.authService = authService;
        this.mistakeService = mistakeService;
    }

    @GetMapping
    public ApiResponse<List<MistakeItem>> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.ok(mistakeService.list(userId));
    }

    @PostMapping
    public ApiResponse<MistakeItem> create(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody CreateMistakeRequest request
    ) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.ok(mistakeService.create(userId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        Long userId = authService.requireUserId(authorization);
        mistakeService.delete(userId, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/analyze")
    public ApiResponse<MistakeAnalysisView> analyze(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.ok(mistakeService.analyze(userId, id));
    }
}
