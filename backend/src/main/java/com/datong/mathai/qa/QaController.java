package com.datong.mathai.qa;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qa/sessions")
public class QaController {

    private final AuthService authService;
    private final QaService qaService;

    public QaController(AuthService authService, QaService qaService) {
        this.authService = authService;
        this.qaService = qaService;
    }

    @PostMapping
    public ApiResponse<QaSessionItem> create(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody(required = false) CreateSessionRequest request
    ) {
        CreateSessionRequest payload = request == null ? new CreateSessionRequest(null) : request;
        return ApiResponse.ok(qaService.createSession(authService.requireUserId(authorization), payload));
    }

    @GetMapping
    public ApiResponse<List<QaSessionItem>> list(
        @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ApiResponse.ok(qaService.listSessions(authService.requireUserId(authorization)));
    }

    @GetMapping("/{sessionId}/messages")
    public ApiResponse<List<QaMessageItem>> messages(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long sessionId
    ) {
        return ApiResponse.ok(qaService.listMessages(authService.requireUserId(authorization), sessionId));
    }

    @PostMapping("/{sessionId}/messages")
    public ApiResponse<QaConversationTurn> send(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long sessionId,
        @Valid @RequestBody SendMessageRequest request
    ) {
        return ApiResponse.ok(qaService.sendMessage(authService.requireUserId(authorization), sessionId, request));
    }
}
