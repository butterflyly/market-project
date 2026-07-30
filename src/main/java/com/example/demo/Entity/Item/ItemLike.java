package com.example.demo.Entity.Item;

import com.example.demo.Entity.Users.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "item_like",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"item_id", "users_id"})
        }
)
public class ItemLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id")
    private Users users;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @CreatedDate
    private LocalDateTime createdAt;

    // 빌더 패턴 사용 시 createdAt 자동 생성 로직
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    protected void setItemPost(Item itemPost) {
        this.item = item;
    }
}