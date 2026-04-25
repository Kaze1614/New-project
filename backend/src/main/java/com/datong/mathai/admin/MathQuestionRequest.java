package com.datong.mathai.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MathQuestionRequest(
    @Size(max = 1024, message = "图片地址不能超过 1024 个字符")
    String imageUrl,

    @NotBlank(message = "题干不能为空")
    String rawTextLatex,

    String answerLatex,

    String teacherExplanation,

    @NotBlank(message = "教材名称不能为空")
    @Size(max = 120, message = "教材名称不能超过 120 个字符")
    String bookName,

    @NotBlank(message = "章节名称不能为空")
    @Size(max = 160, message = "章节名称不能超过 160 个字符")
    String chapterName,

    @NotBlank(message = "小节名称不能为空")
    @Size(max = 180, message = "小节名称不能超过 180 个字符")
    String sectionName,

    @Min(value = 1900, message = "年份不能早于 1900")
    Integer sourceYear,

    @Size(max = 80, message = "试卷来源不能超过 80 个字符")
    String sourcePaper,

    @NotNull(message = "题号不能为空")
    @Min(value = 1, message = "题号必须大于 0")
    Integer questionNo
) {
}
