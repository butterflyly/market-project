package com.example.demo.Repository.Report;

import com.example.demo.Entity.Item.QItem;
import com.example.demo.Entity.Report.QReport;
import com.example.demo.Entity.Report.Report;
import com.example.demo.Entity.Report.ReportStatus;
import com.example.demo.Entity.Users.QUsers;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ReportRepositoryImpl implements ReportRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Page<Report> findByStatus(ReportStatus status, Pageable pageable) {
        QReport qReport = QReport.report;
        QItem qItem = QItem.item;
        QUsers qSeller = QUsers.users;

        // 1. 콘텐츠 조회 (3단계 패치 조인 적용)
        List<Report> content = jpaQueryFactory
                .selectFrom(qReport)
                .join(qReport.reporter).fetchJoin()
                .join(qReport.reportedItem, qItem).fetchJoin()     // item을 qItem으로 지정
                .join(qItem.seller, qSeller).fetchJoin()           // item의 seller를 패치 조인
                .where(qReport.status.eq(status))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(qReport.id.desc())
                .fetch();

        // 2. 카운트 쿼리
        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(qReport.count())
                .from(qReport)
                .where(qReport.status.eq(status));

        // 3. 페이징 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);

    }
}
