package com.example.demo.Entity.Alarm;

public enum NotificationType {

    DEAL_CONFIRMED, // 거래 확정
    CHAT,           // 채팅 도착
    REVIEW,         // 리뷰

    /**
     * 내가 '찜한 상품'의 가격이 떨어졌을 때 (구매자들에게 단체 전송)
     */
    PRICE_DROPPED,

    /**
      내가 '찜한 상품'의 판매 상태가 변경되었을 때 (구매자들에게 전송)
     */
    ITEM_STATUS_CHANGED,

    /**
       시스템 및 공지사항 알림
     */
    SYSTEM
}
