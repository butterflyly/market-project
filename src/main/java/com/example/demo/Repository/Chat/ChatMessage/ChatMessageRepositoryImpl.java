package com.example.demo.Repository.Chat.ChatMessage;

import com.example.demo.Entity.Chat.QChatMessage;
import com.example.demo.Entity.Users.Users;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 1. 안 읽은 메시지 읽음 처리 (UPDATE 벌크 쿼리)
     */
    @Override
    public void markAsReadWithQuerydsl(Long roomId, String username) {
        // 자동 생성된 Q클래스를 불러옴 (SQL의 테이블 역할을 하는 자바 객체)
        QChatMessage m = QChatMessage.chatMessage;

        queryFactory
                .update(m)
                .set(m.isRead, true) // isRead 컬럼을 true 로 변경해줘
                .where(
                        m.chatRoom.id.eq(roomId),       // WHERE chat_room_id = :roomId
                        m.sender.username.ne(username), // AND sender_username != :username (ne는 Not Equal)
                        m.isRead.isFalse()              // AND is_read = false
                )
                .execute(); //  쿼리 최종 실행 (JPQL 의 @Modifying 역할)
    }

    // update ChatMessage cm set cm.sender = :ghostUser where cm.sender.id in :userIds
    @Override
    public void transferSenderToGhost(List<Long> expiredUserIds, Users ghostUser) {
        QChatMessage m = QChatMessage.chatMessage;

        queryFactory
                .update(m)
                .set(m.sender , ghostUser)
                .where(
                        m.sender.id.in(expiredUserIds)
                )
                .execute();
    }
}
