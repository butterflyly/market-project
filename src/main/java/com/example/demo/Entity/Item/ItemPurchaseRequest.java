package com.example.demo.Entity.Item;

import com.example.demo.Entity.Users.Users;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Table(name = "item_purchase_request")
@EntityListeners(AuditingEntityListener.class)

public class ItemPurchaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private Users buyerUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "purchase_status")
    private ItemPurhaseStatus itemPurchaseStatus; // (오타 교정: Purhase -> Purchase)

    @CreatedDate
    private LocalDateTime createdAt;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.itemPurchaseStatus == null) {
            this.itemPurchaseStatus = ItemPurhaseStatus.PENDING; // 기본값 세팅
        }
    }
}
