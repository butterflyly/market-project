package com.example.demo.Repository.Item.ItemLike;

import com.example.demo.Entity.Item.*;
import com.example.demo.Entity.Users.QUsers;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

@RequiredArgsConstructor
public class ItemLikeRepositoryImpl implements ItemLikeRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<ItemLike> findByMyLikePage(String username, Pageable pageable) {


        QItemLike qItemLike = QItemLike.itemLike;
        QItem qItem = QItem.item;     //  조인 성능 최적화를 위한 Q클래스 선언
        QUsers qSeller = QUsers.users; // 상품 판매자까지 한 번에 가져오기 위한 Q클래스 선언

        // 1. 실제 페이지에 보여줄 데이터 목록 조회 (fetchJoin 적용)
        List<ItemLike> content = jpaQueryFactory
                .selectFrom(qItemLike)
                .join(qItemLike.item, qItem).fetchJoin()       //  ItemLike와 Item을 페치 조인!
                .join(qItem.seller, qSeller).fetchJoin()       //  Item 내부의 seller(유저)까지 미리 한방에 페치 조인!
                .where(
                        qItem.deleted.eq(false),                // 아이템 소프트 삭제 값 false
                        qItemLike.users.username.eq(username)
                )
                .orderBy(qItemLike.id.desc())                  // 최신순 정렬
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 카운트 조회 쿼리 준비 (성능 최적화를 위해 패치 조인 생략)
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qItemLike.count())
                .from(qItemLike)
                .join(qItemLike.item, qItem)                   // where절 검증을 위한 일반 조인 유지
                .where(
                        qItem.deleted.eq(false),
                        qItemLike.users.username.eq(username)
                );

        // 3. 스프링 데이터 페이징 유틸로 안전하게 감싸서 리턴
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
