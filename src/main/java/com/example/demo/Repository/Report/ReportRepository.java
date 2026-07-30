package com.example.demo.Repository.Report;

import com.example.demo.Entity.Report.Report;
import com.example.demo.Entity.Report.ReportStatus;
import com.example.demo.Entity.Users.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report,Long> , ReportRepositoryCustom {

    long countByStatus(ReportStatus reportStatus);

    @Modifying
    @Transactional
    @Query("update Report r set r.reporter = :ghostUser where r.reporter.id in :expiredUserIds")
    void transferReporterToGhost(@Param("expiredUserIds") List<Long> expiredUserIds,
                                 @Param("ghostUser") Users ghostUser);

    @Modifying
    @Transactional
    @Query("update Report r set r.reporter = null where r.reporter.id in :expiredUserIds")
    void updateReportedItemToNull(@Param("expiredUserIds") List<Long> expiredItemIds);

    @Query("select r from Report r " +
            "join fetch r.reporter " +
            "join fetch r.reportedItem i " +
            "join fetch i.seller " +
            "where r.id = :id")
    Optional<Report> findDetailById(@Param("id") Long id);
}
