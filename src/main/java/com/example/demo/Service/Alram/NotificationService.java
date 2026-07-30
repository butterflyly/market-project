package com.example.demo.Service.Alram;

import com.example.demo.DTO.Alram.NotificationResponse;
import com.example.demo.Entity.Alarm.Notification;
import com.example.demo.Entity.Alarm.NotificationType;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Repository.Alram.NotificationRepository;
import com.example.demo.Service.Users.UserReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    //  중요: 로그인한 유저들의 실시간 통신 연결선(SseEmitter)들을 보관하는 금고
    // Key: 유저의 username, Value: 브라우저와 연결된 파이프라인
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final NotificationRepository notificationRepository;
    private final UserReadService userReadService;

    // 1. SSE 구독 (504 타임아웃 방지용 초기 이벤트 발송 포함)
    public SseEmitter subscribe(String username) {
        SseEmitter emitter = new SseEmitter(60L * 1000 * 60); // 1시간 동안 연결 유지
        emitters.put(username, emitter); // 금고에 유저 연결선 보관

        // 연결이 끊어지거나 타임아웃 나면 금고에서 삭제하는 안전장치
        emitter.onCompletion(() -> emitters.remove(username));
        emitter.onTimeout(() -> emitters.remove(username));

        // 504 Gateway Timeout 방지를 위한 최초 연결용 Dummy 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("EventStream Connected. [username=" + username + "]"));
        } catch (IOException e) {
            emitters.remove(username);
        }

        return emitter;
    }

    // 2. 알림 전송 (targetUrl 추가)
    public void send(Users receiver, NotificationType type, String content, String targetUrl) {


        //  채팅 알림일 경우, 해당 방의 안 읽은 알림이 이미 있다면 중복 생성 스킵!
        if (type == NotificationType.CHAT) {
            boolean hasUnreadChatNotice = notificationRepository
                    .existsByReceiverAndTargetUrlAndReadFalse(receiver, targetUrl);

            if (hasUnreadChatNotice) {
                log.info(" 이미 읽지 않은 채팅 알림이 존재하므로 알림 생성을 스킵합니다. (targetUrl: {})", targetUrl);
                return;
            }
        }

        // 1. 엔티티 생성
        Notification notification = Notification.builder()
                .receiver(receiver)
                .type(type)
                .message(content)
                .targetUrl(targetUrl) // targetUrl 매핑
                .read(false)
                .build();

        // 2. 이 알림을 DB에 안전하게 먼저 저장
        Notification savedNotification = notificationRepository.save(notification);


        // 3. 수신자의 안 읽은 전체 알림 개수 조회
        long unreadCount = notificationRepository.countByReceiverAndReadFalse(receiver);

        // 4. SSE 전송
        SseEmitter emitter = emitters.get(receiver.getUsername());

        if (emitter != null) {
            try {
                // 만약 NotificationResponse에 unreadCount 필드를 추가했다면 함께 전달
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(NotificationResponse.from(savedNotification, unreadCount)));
            } catch (IOException e) {
                emitters.remove(receiver.getUsername());
            }
        }
    }

    // 3. 읽지 않은 알림 목록 조회 (페이지 상단 드롭다운 노출용)
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(String username) {

        Users user = userReadService.getUserEntity(username);

        return notificationRepository.findByReceiverAndReadFalseOrderByCreatedAtDesc(user)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    // 4. 알림 단건 읽음 처리
    public void markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다."));

        Users user = userReadService.getUserEntity(username);

        if (!notification.getReceiver().getId().equals(user.getId())) {
            throw new IllegalStateException("본인의 알림만 읽음 처리할 수 있습니다.");
        }

        notification.markAsRead(); // Dirty Checking에 의해 자동 update
    }

    // 5. 안 읽은 알림 개수 단독 조회 (추가)
    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {
        Users user = userReadService.getUserEntity(username);
        return notificationRepository.countByReceiverAndReadFalse(user);
    }
}