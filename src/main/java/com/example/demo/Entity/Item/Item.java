package com.example.demo.Entity.Item;

import com.example.demo.Entity.Review.Review;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "items")
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 필요한 것
    // 게시글 제목 , 내용 , 가격, 생성일자 , 수정일자 , 판매자 , 상품 카테고리

    private String title;
    private int price;
    private String content;

    @CreatedDate
    @Column(updatable = false) // 생성 후 수정되지 않도록 보호
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Users seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ItemCategory itemCategory;

    @Builder.Default
    @Column(nullable = false, name = "is_deleted")
    private boolean deleted = false; // 디폴트 false

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemImage> itemImageList = new ArrayList<>();

    @Column(name = "views", nullable = false) // DB에서도 NULL을 불허하도록 설정
    @Builder.Default
    private int views = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus itemStatus = ItemStatus.ON_SALE; // 기본값 직접 대입!

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemLike> itemLike = new ArrayList<>();

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0; // 찜 숫자를 저장할 별도 컬럼

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id") // 외래키 컬럼 추가 (처음엔 null 허용)
    private Users buyer;

    @OneToOne(fetch = FetchType.LAZY) // 상품 하나당 리뷰 하나!
    @JoinColumn(name = "review_id")
    private Review review;
    
    // 관리자 전용
    private boolean isPermanentPreserved = false;

    public boolean hasActiveSeller() {
        // null 이거나 고스트 유저(0L)이면 정상 판매자가 없는 것으로 간주
        if (this.seller == null || Long.valueOf(0L).equals(this.seller.getId())) {
            return false;
        }
        return true;
    }


    public void changeStatus(ItemStatus newStatus, Users buyer) {
        if (this.deleted) {
            throw new BusinessException("삭제된 상품은 상태를 변경할 수 없습니다.");
        }

        if (this.itemStatus == ItemStatus.SOLD_OUT && newStatus == ItemStatus.RESERVED) {
            throw new BusinessException("판매 완료된 상품은 다시 예약 상태로 되돌릴 수 없습니다.");
        }

        //  판매 완료(SOLD_OUT) 상태로 갈 때만 구매자를 매핑해줍니다.
        if (newStatus == ItemStatus.SOLD_OUT || newStatus == ItemStatus.RESERVED) {
            if (buyer == null) {
                throw new BusinessException("판매 완료 처리 시 구매자 정보는 필수입니다.");
            }
            this.buyer = buyer;
        } else {
            // 만약 다시 '판매중'이나 '예약중'으로 돌린다면 구매자 정보를 비워줍니다.
            this.buyer = null;
        }

        this.itemStatus = newStatus;
        this.updatedAt = LocalDateTime.now();
    }


    public void update(String title,String content, int price,
                       ItemCategory itemCategory)
    {
        if (itemCategory == null) {
            throw new BusinessException("카테고리 정보는 필수입니다.");
        }

        this.title =title;
        this.content = content;
        this.price = price;
        this.itemCategory = itemCategory;
        this.updatedAt = LocalDateTime.now();
    }

    // 논리 삭제 메서드
    public void delete() {
        this.updatedAt = LocalDateTime.now();
        this.deleted = true;
    }

    // 상품 판매상태 변경
    public void changeStatus(ItemStatus newStatus) {
        if (this.deleted) {
            throw new BusinessException("삭제된 상품은 상태를 변경할 수 없습니다.");
        }

        if (this.itemStatus == ItemStatus.SOLD_OUT && newStatus == ItemStatus.RESERVED) {
            throw new BusinessException("판매 완료된 상품은 다시 예약 상태로 되돌릴 수 없습니다.");
        }

        // 상태 변경 및 수정일자 갱신
        this.itemStatus = newStatus;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    // 상품 처음 이미지
    public String getThumbnailUrl() {
        return this.itemImageList.stream()
                .findFirst()
                .map(ItemImage::getUrl)
                .orElse("/images/default-item.png");
    }
}
