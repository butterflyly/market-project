package com.example.demo.Repository.Users;

import com.example.demo.Entity.Users.QUsers;
import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<Users> findAdminTargetUsers(String filterType, String keyword, Pageable pageable) {
        QUsers qUsers = QUsers.users;

        // 1. 실제 데이터 조회
        List<Users> content = jpaQueryFactory
                .selectFrom(qUsers)
                .where(
                        getFilterExpression(filterType, qUsers), // 필터 조건 (BLACK, DELETED, ALL)
                        containsKeyword(keyword, qUsers)        // 키워드 검색 조건 (username, nickname, email)
                )
                .orderBy(qUsers.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 카운트 쿼리 (성능 최적화)
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qUsers.count())
                .from(qUsers)
                .where(
                        getFilterExpression(filterType, qUsers),
                        containsKeyword(keyword, qUsers)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 필터 타입("BLACK", "DELETED", "ALL")에 따른 BooleanExpression 반환
     */
    private BooleanExpression getFilterExpression(String filterType, QUsers qUsers) {
        if ("BLACK".equalsIgnoreCase(filterType)) {
            // 블랙 유저이면서 탈퇴(소프트 삭제)하지 않은 유저
            return qUsers.role.eq(UserRole.Black).and(qUsers.deleted.isFalse());
        }
        if ("DELETED".equalsIgnoreCase(filterType)) {
            // 탈퇴한 유저
            return qUsers.deleted.isTrue();
        }
        // "ALL" 이거나 그 외의 경우: 블랙유저이거나 탈퇴한 유저 전체
        return qUsers.role.eq(UserRole.Black).or(qUsers.deleted.isTrue());
    }

    /**
     * 키워드가 존재할 경우 username, nickname, email 중 하나라도 포함(like)하는지 검사
     */
    private BooleanExpression containsKeyword(String keyword, QUsers qUsers) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null; // 조건절에서 완전히 제외됨 (동적 쿼리의 핵심!)
        }
        return qUsers.username.contains(keyword)
                .or(qUsers.nickname.contains(keyword))
                .or(qUsers.email.contains(keyword));
    }
}