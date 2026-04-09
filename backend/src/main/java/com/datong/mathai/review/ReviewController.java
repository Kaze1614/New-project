package com.datong.mathai.review;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review/tasks")
public class ReviewController {

    private final AuthService authService;
    private final ReviewService reviewService;

    public ReviewController(AuthService authService, ReviewService reviewService) {
        this.authService = authService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<List<ReviewTaskView>> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.ok(reviewService.list(authService.requireUserId(authorization)));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<ReviewTaskView> complete(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(reviewService.complete(authService.requireUserId(authorization), id));
    }
}
