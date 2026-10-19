package com.example.demo.Repository.Item.Item;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Item.QItem;
import com.example.demo.Entity.Users.QUsers;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.core.types.Order;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;
    private final EntityManager entityManager;


    @Override
    public Page<Item> findAllByKeyWord(String kw, Long categoryId,
                                                                Pageable pageable, String kw_type) {

        QItem qItem = QItem.item;

        // 1.  실제 페이지에 보여줄 데이터 목록(Content) 조회
        List<Item> content = jpaQueryFactory
                .selectFrom(qItem) // select(qItem).from(qItem)을 한 줄로 줄인 것
                //  연관된 판매자와 카테고리를 한 통속으로 묶어서 한 번에 가져옵니다 (N+1 방지)
                .join(qItem.seller).fetchJoin()
                .join(qItem.itemCategory).fetchJoin()
                .where(
                        qItem.itemCategory.id.eq(categoryId), // 카테고리는 필수 조건
                       // categoryEq(categoryId),                 // 전체 검색도 대응 가능한 동적 메서드()
                        statusEq(ItemStatus.ON_SALE),                //  아이템 상태 동적 조건
                        keywordContains(kw,kw_type) ,                 //  검색어 동적 조건
                        qItem.deleted.eq(false)
                )
                .orderBy(getOrderSpecifiers(pageable.getSort(), qItem)) //  동적 정렬 적용
                .offset(pageable.getOffset())   // 페이징: 시작할 위치 (0부터 시작)
                .limit(pageable.getPageSize())  // 페이징: 한 페이지에 보여줄 개수
                .fetch();                       //  리스트로 결과 받기


        // 2.  조건에 맞는 전체 데이터 개수(Total Count) 조회
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qItem.count()) // SELECT COUNT(id) FROM item
                .from(qItem)
                .where(
                        qItem.itemCategory.id.eq(categoryId),
                        // categoryEq(categoryId),                 // 전체 검색도 대응 가능한 동적 메서드()
                        statusEq(ItemStatus.ON_SALE),
                        keywordContains(kw,kw_type)
                        ,qItem.deleted.eq(false)
                );

        // 3. 두 결과를 스프링 페이징 전용 객체(PageImpl)에 담아서 최종 리턴!
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

    @Override
    public void updateView(Long id) {
        QItem qItem = QItem.item;

        jpaQueryFactory.update(qItem).set(qItem.views, qItem.views.add(1))
                .where(
                        qItem.id.eq(id)
                ).execute();

        entityManager.flush();;
        entityManager.clear();
    }

    @Override
    public void deleteAllByIdsIn(List<Long> itemIds) {

        QItem qItem = QItem.item;

        jpaQueryFactory.delete(qItem)
                .where(qItem.id.in(itemIds))
                .execute();
    }


    @Override
    public Page<Item> findSoftDeletedItemsByKeyword(String keyword, Pageable pageable) {
        QItem qItem = QItem.item;
        QUsers qUsers = qItem.seller; // 관계 추적 가독성을 위해 명시

        // 1. 콘텐츠 조회 쿼리 (페이징 및 패치 조인 적용)
        List<Item> content = jpaQueryFactory
                .selectFrom(qItem)
                .join(qItem.seller, qUsers).fetchJoin() // s.nickname, s.username 접근을 위해 패치 조인
                .where(
                        qItem.deleted.isTrue(),          // 소프트 삭제된 대상 고정
                        searchKeywordContains(keyword)   // 괄호 묶음 동적 조건 메서드 분리 (가독성 업!)
                )
                .offset(pageable.getOffset())            // 앞에 몇 개 건너뛸지
                .limit(pageable.getPageSize())           // 한 페이지에 몇 개 보여줄지
                .orderBy(qItem.id.desc())                // 최신 등록 순 정렬 예시
                .fetch();

        // 2. 카운트 조회 쿼리 (성능 최적화를 위해 패치 조인 생략)
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qItem.count())
                .from(qItem)
                .join(qItem.seller, qUsers) // 조건절에 seller 가 쓰이므로 일반 조인은 유지
                .where(
                        qItem.deleted.isTrue(),
                        searchKeywordContains(keyword)
                );

        // 3. 스프링 데이터 페이징 유틸로 안전하게 감싸서 리턴
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }

    @Override
    public Page<Item> findSoftDeletedItemsAll(Pageable pageable) {

        QItem qItem = QItem.item;

        // 1. 콘텐츠 조회 쿼리 (페이징 및 패치 조인 적용)
        List<Item> content = jpaQueryFactory
                .selectFrom(qItem)
                .where(
                        qItem.deleted.isTrue()          // 소프트 삭제된 대상 고정
                )
                .offset(pageable.getOffset())            // 앞에 몇 개 건너뛸지
                .limit(pageable.getPageSize())           // 한 페이지에 몇 개 보여줄지
                .orderBy(qItem.id.desc())                // 최신 등록 순 정렬 예시
                .fetch();

        // 2. 카운트 조회 쿼리 (성능 최적화를 위해 패치 조인 생략)
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qItem.count())
                .from(qItem)
                .where(
                        qItem.deleted.isTrue()
                );

        // 3. 스프링 데이터 페이징 유틸로 안전하게 감싸서 리턴
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }


    @Override
    public Optional<Item> findSoftDeletedItemDetailById(Long id) {
        QItem qItem = QItem.item;

        Item result = jpaQueryFactory
                .selectFrom(qItem)
                .where(
                        qItem.id.eq(id),
                        qItem.deleted.isTrue()
                )
                .fetchOne();                           // 단건 조회는 fetchOne() 사용

        return Optional.ofNullable(result);            // 결과가 없으면 Optional.empty() 반환
    }

    // 카테고리 필터링
    private BooleanExpression categoryEq(Long categoryId) {

        return categoryId != null ? QItem.item.itemCategory.id.eq(categoryId) : null;
    }

    //  아이템 상태 필터링
    private BooleanExpression statusEq(ItemStatus itemStatus) {
        // 컨트롤러에서 상태 필터를 안 골라서 null이 들어오면 WHERE 절에서 아예 무시(패스)
        return itemStatus != null ? QItem.item.itemStatus.eq(itemStatus) : null;
    }

    // 키워드 검색
    private BooleanExpression keywordContains(String kw,String kw_type) {
        // 검색어가 비어있거나 공백이면 WHERE 절에서 검색 조건 제외 -> 전체 조회 발동!
        if (kw == null || kw.trim().isEmpty()) {
            return null;
        }

        QItem qItem = QItem.item;

        // 2. 검색 타입(searchType)이 무엇이냐에 따라 각각 다른 조건 상자를 리턴
        if ("title".equals(kw_type)) {
            return qItem.title.contains(kw);
        }

        if ("titleAndContent".equals(kw_type)) {
            return qItem.content.contains(kw).or(qItem.title.contains(kw));
        }

        if ("nickname".equals(kw_type)) {
            return qItem.seller.displayNickname.contains(kw);
        }

        // 3. 만약 타입이 지정되지 않았거나 올바르지 않다면?
        // 안전장치로 제목 + 내용 통합 검색을 돌리거나 null 을 리턴
        return qItem.title.contains(kw).or(qItem.content.contains(kw));
    }

    // 관리자 전용 키워드 검색
    private BooleanExpression searchKeywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null; // 키워드가 없으면 무시 (전체 조회처럼 동작)
        }

        QItem qItem = QItem.item;

        // .contains()를 쓰면 SQL의 like '%키워드%' 로 자동 치환
        return qItem.title.contains(keyword)
                .or(qItem.content.contains(keyword))
                .or(qItem.seller.displayNickname.contains(keyword))
                .or(qItem.seller.username.contains(keyword));
    }

    /**
     *  Spring Data JPA의 Sort 객체를 Querydsl의 OrderSpecifier 배열로 변환하는 헬퍼 메서드
     */
    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort, QItem qItem) {
        List<OrderSpecifier<?>> orders = new ArrayList<>();

        if (sort != null && !sort.isEmpty()) {
            for (Sort.Order order : sort) {
                Order direction = order.isAscending() ? Order.ASC : Order.DESC;

                // Pageable에 담겨온 속성명(property)에 맞춰 조건 분기
                switch (order.getProperty()) {
                    case "viewCount" -> orders.add(new OrderSpecifier<>(direction, qItem.views));
                    case "price" -> orders.add(new OrderSpecifier<>(direction, qItem.price));
                    case "createdAt" -> orders.add(new OrderSpecifier<>(direction, qItem.createdAt));
                    case "id" -> orders.add(new OrderSpecifier<>(direction, qItem.id));
                    default -> orders.add(new OrderSpecifier<>(Order.DESC, qItem.id));
                }
            }
        }

        // 기본 정렬값이 비어있을 경우 최신순(id DESC)을 기본값으로 지정
        if (orders.isEmpty()) {
            orders.add(new OrderSpecifier<>(Order.DESC, qItem.id));
        }

        return orders.toArray(new OrderSpecifier[0]);
    }
}
