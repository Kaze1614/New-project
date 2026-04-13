package com.datong.mathai.favorite;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final AuthService authService;
    private final FavoriteService favoriteService;

    public FavoriteController(AuthService authService, FavoriteService favoriteService) {
        this.authService = authService;
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ApiResponse<List<FavoriteItem>> list(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam(required = false) Long chapterId,
        @RequestParam(required = false) String difficulty,
        @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.ok(favoriteService.list(authService.requireUserId(authorization), chapterId, difficulty, keyword));
    }

    @PostMapping
    public ApiResponse<FavoriteItem> create(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody CreateFavoriteRequest request
    ) {
        return ApiResponse.ok(favoriteService.create(authService.requireUserId(authorization), request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        favoriteService.delete(authService.requireUserId(authorization), id);
        return ApiResponse.ok(null);
    }
}
