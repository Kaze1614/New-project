package com.datong.mathai.search;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final AuthService authService;
    private final SearchService searchService;

    public SearchController(AuthService authService, SearchService searchService) {
        this.authService = authService;
        this.searchService = searchService;
    }

    @GetMapping
    public ApiResponse<SearchResult> search(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam String keyword
    ) {
        return ApiResponse.ok(searchService.search(authService.requireUserId(authorization), keyword));
    }
}
