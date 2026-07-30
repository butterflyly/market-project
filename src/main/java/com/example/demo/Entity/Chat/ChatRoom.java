package com.example.demo.Entity.Chat;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static com.example.demo.Entity.Chat.ChatRoomStatus.BOTH_IN;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private Users buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Users seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @CreatedDate
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING) // DB에 "BOTH_IN" 같은 문자열로 저장됩니다.
    @Builder.Default
    private ChatRoomStatus status = BOTH_IN;

    public Users getPartner(String loginUsername) {
        return this.buyer.getUsername().equals(loginUsername) ? this.seller : this.buyer;
    }

    public void buyerLeave() {
        // 구매자가 나갔을 때, 이미 판매자도 나간 상태(HOST_OUT)였다면 -> 둘 다 나감(BOTH_OUT)
        // 판매자는 아직 안 나간 상태였다면 -> 구매자만 나감(GUEST_OUT)
        this.status = (this.status == ChatRoomStatus.HOST_OUT)
                ? ChatRoomStatus.BOTH_OUT
                : ChatRoomStatus.GUEST_OUT;
    }

    public void sellerLeave() {
        // 판매자가 나갔을 때, 이미 구매자가 나간 상태(GUEST_OUT)였다면 -> 둘 다 나감(BOTH_OUT)
        // 구매자는 아직 안 나간 상태였다면 -> 판매자만 나감(HOST_OUT)
        this.status = (this.status == ChatRoomStatus.GUEST_OUT)
                ? ChatRoomStatus.BOTH_OUT
                : ChatRoomStatus.HOST_OUT;
    }

    //  서비스가 "둘 다 나갔는지" 아주 쉽게 물어볼 수 있는 서브 메서드
    public boolean isBothLeft() {
        return this.status == ChatRoomStatus.BOTH_OUT;
    }

    // 만약 지난번에 작성한 buyer_leave 에서 다룬 Status 기준이라면:
    public boolean isBuyerLeft() {
        // 구매자가 나가서 GUEST_OUT 이 되었거나, 둘 다 나가서 BOTH_OUT 인 경우
        return this.status == ChatRoomStatus.GUEST_OUT || this.status == ChatRoomStatus.BOTH_OUT;
    }

    public boolean isSellerLeft() {
        // 판매자가 나가서 HOST_OUT 이 되었거나, 둘 다 나가서 BOTH_OUT 인 경우
        return this.status == ChatRoomStatus.HOST_OUT || this.status == ChatRoomStatus.BOTH_OUT;
    }
}
