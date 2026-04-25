package com.datong.mathai.review;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final AuthService authService;
    private final ReviewService reviewService;

    public ReviewController(AuthService authService, ReviewService reviewService) {
        this.authService = authService;
        this.reviewService = reviewService;
    }

    @GetMapping("/tasks")
    public ApiResponse<List<ReviewTaskView>> list(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(required = false) String scope
    ) {
        return ApiResponse.ok(reviewService.list(authService.requireUserId(authorization), scope));
    }

    @GetMapping("/next")
    public ApiResponse<ReviewTaskView> next(
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(reviewService.next(authService.requireUserId(authorization)));
    }

    @PostMapping("/tasks/{id}/complete")
    public ApiResponse<ReviewTaskView> complete(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(reviewService.complete(authService.requireUserId(authorization), id));
    }

    @PostMapping("/tasks/{id}/rate")
    public ApiResponse<ReviewTaskView> rate(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id,
        @Valid @RequestBody ReviewRateRequest request
    ) {
        return ApiResponse.ok(reviewService.rate(authService.requireUserId(authorization), id, request.grade()));
    }

    @PostMapping("/submit")
    public ApiResponse<ReviewSubmitResponse> submit(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody ReviewSubmitRequest request
    ) {
        return ApiResponse.ok(reviewService.submit(authService.requireUserId(authorization), request));
    }
}
