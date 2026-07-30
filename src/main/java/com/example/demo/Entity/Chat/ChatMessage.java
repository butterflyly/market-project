package com.example.demo.Entity.Chat;

import com.example.demo.Entity.Users.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private Users sender;

    private String senderNickname;

    @Column(columnDefinition = "TEXT") // 긴 메시지를 대비해 TEXT 타입 권장
    private String content;

    @CreatedDate
    private LocalDateTime createdAt;

    private boolean isRead; // 읽음 처리 여부

    @Enumerated(EnumType.STRING)
    private MessageType type; // TALK 또는 IMAGE

    @Builder
    public ChatMessage(ChatRoom chatRoom, Users sender, String content, boolean isRead, MessageType type) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.content = content;
        this.isRead = isRead;
        this.type = type != null ? type : MessageType.TALK;
        // 생성 시점 스냅샷 안전하게 캡처
        this.senderNickname = sender != null ? sender.getDisplayNickname() : "탈퇴사용자";
    }
}
