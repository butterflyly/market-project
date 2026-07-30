package com.example.demo.Entity.Report;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportType {

    SPAM("광고성 게시글", "홍보, 도배, 카테고리 도배 등"),
    SCAM("사기 의심", "예약금 먹튀, 가짜 안전결제 유도, 위조품 판매 등"),
    BAD_MANNERS("욕설 및 비매너", "비속어 사용, 불쾌한 언행, 성희롱 등"),
    PROHIBITED_ITEM("판매 금지 물품 등록", "의약품, 주류/담배, 영리 목적의 되팔이 등"),
    OTHER("기타 사유", "그 외 운영 정책 위반 사항");

    private final String title;       // 화면에 보여줄 이쁜 제목
    private final String description; // 상세 설명 (가이드용)
}