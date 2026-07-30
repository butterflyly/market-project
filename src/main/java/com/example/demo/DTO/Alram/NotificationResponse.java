package com.example.demo.DTO.Alram;

import com.example.demo.Entity.Alarm.Notification;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record NotificationResponse(
        Long id,
        String message,   // 예: "말랑카우님이 새로운 채팅을 보냈습니다."
        String content,   //  프론트엔드(JS) notification.content 호환용
        String targetUrl, // 예: "/chat/room/1"
        String type,      // 예: "CHAT", "ITEM_STATUS_CHANGED"
        boolean isRead,   // 읽음 여부
        String createdAt, // 예: "방금 전", "10분 전"
        Long unreadCount  //  안 읽은 전체 알림 개수
) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // 단건 조회/목록 조회용 (unreadCount 없음)
    public static NotificationResponse from(Notification notification) {
        return from(notification, null);
    }

    // 실시간 SSE 알림 발송용 (unreadCount 포함)
    public static NotificationResponse from(Notification notification, Long unreadCount) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMessage(),
                notification.getMessage(), // content 필드에도 동일하게 message 값 할당
                notification.getTargetUrl(),
                notification.getType() != null ? notification.getType().name() : "SYSTEM",
                notification.isRead(),
                formatCreatedAt(notification.getCreatedAt()),
                unreadCount
        );
    }

    private static String formatCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) return "-";

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(createdAt, now);
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "방금 전";
        } else if (seconds < 3600) {
            return (seconds / 60) + "분 전";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "시간 전";
        } else if (seconds < 86400 * 7) {
            return (seconds / 86400) + "일 전";
        } else {
            return createdAt.format(FORMATTER);
        }
    }
}