package com.example.demo.Config;

import com.example.demo.DTO.Alram.*;
import com.example.demo.Entity.Alarm.NotificationType;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemLike;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Repository.Item.ItemLike.ItemLikeRepository;
import com.example.demo.Service.Alram.NotificationService;
import com.example.demo.Service.Users.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

import java.util.List;

import static com.example.demo.Entity.Alarm.NotificationType.CHAT;
import static com.example.demo.Entity.Alarm.NotificationType.ITEM_STATUS_CHANGED;
import static com.example.demo.Entity.Users.MyPageTab.SELLING;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final ItemLikeRepository itemLikeRepository;
    private final UserReadService userReadService;

    // 1. 찜 유저 대상 - 판매 상태 변경 알림
    @Async
    @EventListener
    public void handleItemStatusChanged(ItemStatusChangedEvent event) {
        Item item = event.item();
        ItemStatus newStatus = item.getItemStatus();

        // 상태값에 따른 가독성 있는 한글 텍스트 변환
        String statusText = switch (newStatus) {
            case RESERVED -> "예약중";
            case SOLD_OUT -> "판매완료";
            case ON_SALE -> "판매중";
            default -> "상태 변경";
        };

        List<ItemLike> likes = itemLikeRepository.findByItem(item);

        for (ItemLike like : likes) {
            Users receiver = like.getUsers();

            String message = String.format("회원님이 찜하신 '%s' 상품이 [%s] 상태로 변경되었습니다.", item.getTitle(), statusText);

            notificationService.send(receiver, NotificationType.ITEM_STATUS_CHANGED, message, event.targetUrl());
        }
    }

    // 2. 구매자 대상 - 거래 완료 및 리뷰 작성 유도 알림
    @Async
    @EventListener
    public void handleDealConfirmed(DealConfirmedEvent event) {

        Users buyer = event.buyer(); // 또는 event.buyerId()로 조회된 유저
        String sellerNickname = event.sellerNickname();
        String itemTitle = event.title();
        String targetUrl = event.targetUrl();

        String message = String.format("'%s'님과의 [%s] 거래가 완료되었습니다. 따뜻한 후기를 남겨주세요!", sellerNickname, itemTitle);

        notificationService.send(buyer, NotificationType.DEAL_CONFIRMED, message ,targetUrl);
    }

    // 3. 채팅도착 알림 이벤트 수신
    @Async
    @EventListener
    public void handleChatEvent(ChatNotificationEvent event) {
        // 수신자 ID로 유저 조회
        Users receiver = userReadService.getUserEntityById(event.receiverId());

        String message = String.format("%s님이 새로운 메시지를 보냈습니다.", event.senderNickname());

        // DB 저장 + SSE 발송을 NotificationService.send() 하나로 처리
        notificationService.send(
                receiver,
                NotificationType.CHAT,
                message,
                event.roomUrl()
        );
    }

    // 4. 신고 처리 완료 알림 이벤트 수신
    @Async
    @EventListener
    public void handleReportProcessed(ReportProcessedEvent event) {

        notificationService.send(
                event.reporter(),
                NotificationType.SYSTEM,
                event.message(),
                "/items/list/1" // 임시로 메인페이지로 보내기 (내 신고내역 페이지가 현재 없음)
        );
    }

    // 5. 경고 수신 알림 이벤트 수신
    @Async
    @EventListener
    public void handleUserWarning(UserWarningEvent event) {
        Users warnedUser = event.warnedUser();

        String message;
        if (event.isBlackened()) {
            message = "운영 정책 위반으로 경고가 누적되어 계정이 블랙리스트(이용 제한) 상태로 전환되었습니다.";
        } else {
            message = String.format("운영 정책 위반으로 경고 1회가 부여되었습니다. (현재 누적 경고: %d회 / 3회 이상 시 이용 제한)", event.warningCount());
        }

        notificationService.send(
                warnedUser,
                NotificationType.SYSTEM,
                message,
                "/items/list/1" // 임시로 메인페이지로 보내기 (내 신고내역 페이지가 현재 없음)
        );
    }
}