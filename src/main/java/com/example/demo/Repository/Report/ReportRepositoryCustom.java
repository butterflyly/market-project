package com.example.demo.Repository.Report;

import com.example.demo.Entity.Report.Report;
import com.example.demo.Entity.Report.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.Optional;

public interface ReportRepositoryCustom {

    //  관리자용: 신고 목록 전체 조회 (필드명 수정 및 countQuery 최적화)
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

}
