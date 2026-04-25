package com.datong.mathai.admin;

import com.datong.mathai.auth.AuthService;
import com.datong.mathai.common.ApiResponse;
import com.datong.mathai.common.AppException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/uploads")
public class AdminUploadController {

    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final AuthService authService;

    public AdminUploadController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/question-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResponse> uploadQuestionImage(
        @RequestHeader(value = "Authorization", required = false) String authorization,
        @RequestParam("file") MultipartFile file
    ) {
        authService.requireAdminUserId(authorization);
        if (file.isEmpty()) {
            throw new AppException(400, "图片不能为空");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new AppException(400, "图片不能超过 10MB");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new AppException(400, "仅支持 png、jpg、jpeg、webp 图片");
        }

        LocalDate today = LocalDate.now();
        String folder = String.format("uploads/questions/%04d/%02d", today.getYear(), today.getMonthValue());
        Path targetDir = Path.of(folder).toAbsolutePath().normalize();
        try {
            Files.createDirectories(targetDir);
            String filename = UUID.randomUUID().toString().replace("-", "") + extension(file.getOriginalFilename(), contentType);
            Path target = targetDir.resolve(filename).normalize();
            file.transferTo(target);
            String url = "/" + folder.replace('\\', '/') + "/" + filename;
            return ApiResponse.ok(new UploadResponse(url, filename, file.getSize()));
        } catch (IOException ex) {
            throw new AppException(500, "图片保存失败");
        }
    }

    private String extension(String originalName, String contentType) {
        if (originalName != null) {
            String lower = originalName.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".png")) return ".png";
            if (lower.endsWith(".jpg")) return ".jpg";
            if (lower.endsWith(".jpeg")) return ".jpeg";
            if (lower.endsWith(".webp")) return ".webp";
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
