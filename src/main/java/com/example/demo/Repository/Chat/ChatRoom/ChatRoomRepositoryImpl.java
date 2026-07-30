package com.example.demo.Repository.Chat.ChatRoom;

import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Chat.ChatRoomStatus;
import com.example.demo.Entity.Chat.QChatRoom;
import com.example.demo.Entity.Item.QItem;
import com.example.demo.Entity.Users.QUsers;
import com.example.demo.Entity.Users.Users;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom{

    private final JPAQueryFactory queryFactory;


    @Override
    public List<ChatRoom> findAllActiveRoomsByUserId(Long id) {

        QChatRoom cr = QChatRoom.chatRoom;

       return queryFactory.selectFrom(cr)
                .where(
                        // 1. 내가 구매자(buyer)이고, 판매자만 나갔거나 둘 다 들어와 있는 상태
                        (cr.buyer.id.eq(id).and(cr.status.in(ChatRoomStatus.BOTH_IN, ChatRoomStatus.HOST_OUT)))
                                .or(
                        // 2. 내가 판매자(seller)이고, 구매자만 나갔거나 둘 다 들어와 있는 상태
                        cr.seller.id.eq(id).and(cr.status.in(ChatRoomStatus.BOTH_IN, ChatRoomStatus.GUEST_OUT)))
                ).fetch();
    }

    @Override
    public void transferBuyerToGhost(List<Long> userIds, Users ghostUser) {
        QChatRoom cr = QChatRoom.chatRoom;

        queryFactory.update(cr)
                .set(cr.buyer , ghostUser)
                .where(
                        cr.buyer.id.in(userIds)
                ).execute();
    }

    @Override
    public void transferSellerToGhost(List<Long> userIds, Users ghostUser) {
        QChatRoom cr = QChatRoom.chatRoom;

        queryFactory.update(cr)
                .set(cr.seller, ghostUser)
                .where(
                        cr.seller.id.in(userIds)
                ).execute();
    }

    @Override
    public void deletePureGhostRooms(Long ghostUserId) {
        QChatRoom cr = QChatRoom.chatRoom;

        queryFactory.delete(cr)
                .where(
                        cr.buyer.id.eq(ghostUserId)
                                .and(cr.seller.id.eq(ghostUserId))
                ).execute();
    }

    @Override
    public Page<ChatRoom> findAllByBuyerAndStatusIn(Users buyer, List<ChatRoomStatus> statuses, Pageable pageable) {
        QChatRoom cr = QChatRoom.chatRoom;
        QItem item = QItem.item;
        QUsers seller = new QUsers("sellerUser"); // SQL 별칭: sellerUser

        // [콘텐츠 조회] 패치 조인으로 N+1 완벽 차단 및 페이징 적용
        List<ChatRoom> content = queryFactory
                .selectFrom(cr)
                .join(cr.item, item).fetchJoin()
                .join(cr.seller, seller).fetchJoin() // cr.seller 정보를 가져올 때 sellerUser 라는 별칭으로 묶음
                .where(
                        cr.buyer.eq(buyer), // cr.buyer 는 별칭 분리 없이 바로 비교 가능
                        cr.status.in(statuses)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize()) // 제한
                .orderBy(cr.id.desc()) // 최근 생성된 방이 위로 오도록 정렬 (기획에 맞게 수정 가능)
                .fetch();

        // [카운트 쿼리 분리] 데이터가 한 페이지 미만이거나 마지막 페이지일 때 카운트 쿼리를 생략하여 성능 최적화
        JPAQuery<Long> countQuery = queryFactory
                .select(cr.count())
                .from(cr)
                .where(
                        cr.buyer.eq(buyer),
                        cr.status.in(statuses)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<ChatRoom> findAllBySellerAndStatusIn(Users seller, List<ChatRoomStatus> statuses, Pageable pageable) {
        QChatRoom cr = QChatRoom.chatRoom;
        QItem item = QItem.item;
        QUsers buyer = new QUsers("buyerUser");

        List<ChatRoom> content = queryFactory
                .selectFrom(cr)
                .join(cr.item, item).fetchJoin()    //  상품 정보 한방에 로딩
                .join(cr.buyer, buyer).fetchJoin()  //  상대방(구매자) 정보 한방에 로딩
                .where(
                        cr.seller.eq(seller),
                        cr.status.in(statuses)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(cr.id.desc())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(cr.count())
                .from(cr)
                .where(
                        cr.seller.eq(seller),
                        cr.status.in(statuses)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }


}
