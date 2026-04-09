package com.datong.mathai.dashboard;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final AuthService authService;
    private final DashboardService dashboardService;

    public DashboardController(AuthService authService, DashboardService dashboardService) {
        this.authService = authService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverview> overview(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = authService.requireUserId(authorization);
        return ApiResponse.ok(dashboardService.overview(userId));
    }
}
