package com.datong.mathai.chapter;

import com.datong.mathai.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping("/tree")
    public ApiResponse<List<ChapterNode>> tree() {
        return ApiResponse.ok(chapterService.tree());
    }
}
