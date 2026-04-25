package com.datong.mathai.admin;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AuthService authService;
    private final AdminUserService adminUserService;

    public AdminUserController(AuthService authService, AdminUserService adminUserService) {
        this.authService = authService;
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminUserItem>> list(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        authService.requireAdminUserId(authorization);
        return ApiResponse.ok(adminUserService.list(keyword, page, size));
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        authService.requireAdminUserId(authorization);
        adminUserService.resetPassword(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        Long currentUserId = authService.requireAdminUserId(authorization);
        adminUserService.delete(currentUserId, id);
        return ApiResponse.ok(null);
    }
}
