package com.example.demo.Controller.Alram;

import com.example.demo.DTO.Alram.NotificationResponse;
import com.example.demo.DTO.CommonResponse;
import com.example.demo.Security.Custom.CustomUserDetails;
import com.example.demo.Service.Alram.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService; // SSE 연결 관리 서비스

    // 1. 실시간 SSE 연결
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            throw new IllegalArgumentException("인증된 사용자 정보가 없습니다.");
        }

        return notificationService.subscribe(userDetails.getUsername());
    }

    // 2. 읽지 않은 알림 목록 조회
    @GetMapping
    public ResponseEntity<CommonResponse<List<NotificationResponse>>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<NotificationResponse> notifications = notificationService.getUnreadNotifications(userDetails.getUsername());
        return ResponseEntity.ok(new CommonResponse<>(true, "읽지 않은 알림 목록 조회 성공", notifications));
    }

    // 3. 알림 읽음 처리
    @PatchMapping("/{id}/read")
    public ResponseEntity<CommonResponse<Void>> markAsRead(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        notificationService.markAsRead(id, userDetails.getUsername());
        return ResponseEntity.ok(new CommonResponse<>(true, "알림 읽음 처리 완료", null));
    }

    // 4. 안 읽은 알림 개수만 단독 조회 API (추가)
    @GetMapping("/unread-count")
    public ResponseEntity<CommonResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {

        long count = notificationService.getUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(new CommonResponse<>(true, "안 읽은 알림 개수 조회 성공", count));
    }
}