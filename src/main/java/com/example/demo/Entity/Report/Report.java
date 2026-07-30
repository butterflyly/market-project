package com.example.demo.Entity.Report;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Users reporter; // 신고자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private Users reportedUser; // 피신고자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_item_id") // Nullable
    private Item reportedItem; // 신고된 게시글 (선택)

    @Enumerated(EnumType.STRING)
    private ReportType reportType; // 신고 카테고리 Enum

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 상세 사유

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING; // 처리 상태 (대기/완료)

    @CreatedDate
    private LocalDateTime createdAt;


    private LocalDateTime processedAt; // 관리자가 처리한 시간

    //  관리자가 [처리 완료] 버튼을 눌렀을 때 상태를 변경하는 도메인 메서드
    public void completeProcessing() {
        this.status = ReportStatus.PROCESSED;
        this.processedAt = LocalDateTime.now();
    }
}