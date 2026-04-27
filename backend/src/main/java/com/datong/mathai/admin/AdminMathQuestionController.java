package com.datong.mathai.admin;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/math-questions")
public class AdminMathQuestionController {

    private final AuthService authService;
    private final AdminMathQuestionService mathQuestionService;

    public AdminMathQuestionController(AuthService authService, AdminMathQuestionService mathQuestionService) {
        this.authService = authService;
        this.mathQuestionService = mathQuestionService;
    }

    @GetMapping
    public ApiResponse<PageResult<MathQuestionListItem>> list(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long bookId,
        @RequestParam(required = false) Long chapterId,
        @RequestParam(required = false) Long sectionId,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        authService.requireAdminUserId(authorization);
        return ApiResponse.ok(mathQuestionService.list(keyword, bookId, chapterId, sectionId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MathQuestionDetail> get(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        authService.requireAdminUserId(authorization);
        return ApiResponse.ok(mathQuestionService.get(id));
    }

    @PostMapping
    public ApiResponse<MathQuestionDetail> create(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody MathQuestionRequest request
    ) {
        authService.requireAdminUserId(authorization);
        return ApiResponse.ok(mathQuestionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<MathQuestionDetail> update(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id,
        @Valid @RequestBody MathQuestionRequest request
    ) {
        authService.requireAdminUserId(authorization);
        return ApiResponse.ok(mathQuestionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        authService.requireAdminUserId(authorization);
        mathQuestionService.delete(id);
        return ApiResponse.ok(null);
    }
}
