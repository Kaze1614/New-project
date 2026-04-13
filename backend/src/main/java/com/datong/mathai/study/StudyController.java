package com.datong.mathai.study;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/study/sessions")
public class StudyController {

    private final AuthService authService;
    private final StudyService studyService;

    public StudyController(AuthService authService, StudyService studyService) {
        this.authService = authService;
        this.studyService = studyService;
    }

    @PostMapping
    public ApiResponse<StudySessionDetail> create(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @Valid @RequestBody(required = false) CreateStudySessionRequest request
    ) {
        CreateStudySessionRequest payload = request == null ? new CreateStudySessionRequest(null, null) : request;
        return ApiResponse.ok(studyService.createSession(authService.requireUserId(authorization), payload));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudySessionDetail> detail(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(studyService.getSession(authService.requireUserId(authorization), id));
    }

    @PostMapping("/{id}/answers")
    public ApiResponse<StudySessionDetail> saveAnswer(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id,
        @Valid @RequestBody SaveStudyAnswerRequest request
    ) {
        return ApiResponse.ok(studyService.saveAnswer(authService.requireUserId(authorization), id, request));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<StudySessionDetail> submit(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable Long id
    ) {
        return ApiResponse.ok(studyService.submit(authService.requireUserId(authorization), id));
    }
}
