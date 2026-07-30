package com.example.demo.DTO.Report;

import com.example.demo.Entity.Report.Report;
import com.example.demo.Entity.Report.ReportStatus;
import com.example.demo.Entity.Report.ReportType;

import java.time.LocalDateTime;

public record ReportResponse(
        Long id,
        ReportType reportType,
        String reportTypeDescription,
        ReportStatus status,
        LocalDateTime createdAt,
        Long targetItemId,
        String targetItemTitle,
        String reporterNickname,
        String sellerUsername,
        String sellerNickname
) {
    public static ReportResponse from(Report report) {

        String description = switch (report.getReportType()) {
            case SPAM -> "광고성 게시글";
            case SCAM -> "사기 의심";
            case BAD_MANNERS -> "욕설 및 비매너";
            case PROHIBITED_ITEM -> "판매 금지 물품";
            default -> "기타 신고 사유";
        };

        // 안전 분기 조건 계산
        boolean hasReporter = report.getReporter() != null;
        boolean hasItem = report.getReportedItem() != null;
        boolean hasSeller = hasItem && report.getReportedItem().getSeller() != null;

        return new ReportResponse(
                report.getId(),
                report.getReportType(),
                description,
                report.getStatus(),
                report.getCreatedAt(),

                hasItem ? report.getReportedItem().getId() : null,
                hasItem ? report.getReportedItem().getTitle() : "삭제된 상품입니다.",
                hasReporter ? report.getReporter().getNickname() : "탈퇴사용자",
                hasSeller ? report.getReportedItem().getSeller().getUsername() : "unknown",
                hasSeller ? report.getReportedItem().getSeller().getNickname() : "탈퇴사용자"
        );
    }
}