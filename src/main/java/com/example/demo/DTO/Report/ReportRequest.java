package com.example.demo.DTO.Report;

import com.example.demo.Entity.Report.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportRequest(
        @NotNull(message = "신고 카테고리를 선택해주세요.")
        ReportType reportType,

        @NotBlank(message = "상세 신고 사유를 적어주세요.")
        String content
) {}