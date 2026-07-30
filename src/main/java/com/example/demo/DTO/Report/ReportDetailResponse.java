package com.example.demo.DTO.Report;

import com.example.demo.Entity.Report.Report;
import com.example.demo.Entity.Report.ReportStatus;

import java.time.LocalDateTime;

public record ReportDetailResponse(
        Long id,
        String reportTypeDescription,
        String content,
        ReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        Long reporterId,
        String reporterUsername,
        String reporterNickname,
        Long sellerId,
        String sellerUsername,
        String sellerNickname,
        Long targetItemId,
        String targetItemTitle,
        int targetItemPrice
) {
    public static ReportDetailResponse from(Report report) {
        String description = switch (report.getReportType()) {
            case SPAM -> "광고성 게시글 (홍보, 도배 등)";
            case SCAM -> "사기 의심 (예약금 먹튀, 위조품 등)";
            case BAD_MANNERS -> "욕설 및 비매너";
            case PROHIBITED_ITEM -> "판매 금지 물품 등록";
            default -> "기타 신고 사유";
        };

        // 하드 탈퇴로 인한 연관 객체 null 체크 방어막 구축
        boolean hasReporter = report.getReporter() != null;
        boolean hasItem = report.getReportedItem() != null;
        boolean hasSeller = hasItem && report.getReportedItem().getSeller() != null;

        return new ReportDetailResponse(
                report.getId(),
                description,
                report.getContent(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getProcessedAt(),

                // 신고자 정보 처리
                hasReporter ? report.getReporter().getId() : null,
                hasReporter ? report.getReporter().getUsername() : "unknown",
                hasReporter ? report.getReporter().getNickname() : "탈퇴사용자",

                // 피신고자(판매자) 정보 처리
                hasSeller ? report.getReportedItem().getSeller().getId() : null,
                hasSeller ? report.getReportedItem().getSeller().getUsername() : "unknown",
                hasSeller ? report.getReportedItem().getSeller().getNickname() : "탈퇴사용자",

                // 신고 대상 상품 정보 처리
                hasItem ? report.getReportedItem().getId() : null,
                hasItem ? report.getReportedItem().getTitle() : "삭제된 상품입니다.",
                hasItem ? report.getReportedItem().getPrice() : 0
        );
    }
}