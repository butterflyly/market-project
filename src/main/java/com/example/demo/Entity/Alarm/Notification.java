package com.example.demo.Entity.Alarm;


import com.example.demo.Entity.Item.ItemStatus;
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
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private Users receiver;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;
    private String targetUrl;

    @Column(name = "is_read")
    private boolean read;

    @CreatedDate
    private LocalDateTime createdAt;

    public void markAsRead() {
        this.read = true;
    }
}
