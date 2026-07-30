package com.example.demo.Entity.Review;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) // 상품 하나당 리뷰 하나!
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private Users writer; // 구매자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private Users receiver; // 판매자

    @Column(nullable = false)
    private int rating; // 1 ~ 5 (DTO validator @Min(1) @Max(5)와 별개로 처리)

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void update(int rating, String content) {
        this.rating = rating;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     *  정상적인 작성자가 존재하는지 확인. (null 및 고스트 유저 체크)
     */
    public boolean hasActiveWriter() {
        if (this.writer == null || Long.valueOf(0L).equals(this.writer.getId())) {
            return false;
        }
        return true;
    }

    /**
     * 정상적인 상품이 존재하는지 확인합
     */
    public boolean hasActiveItem() {
        if (this.item == null) { // 만약 상품도 소프트 삭제/고스트 처리가 있다면 조건 추가 가능
            return false;
        }
        return true;
    }
}