package com.example.demo.Service.Report;


import com.example.demo.DTO.Report.ReportDetailResponse;
import com.example.demo.DTO.Report.ReportResponse;
import com.example.demo.Entity.Report.Report;
import com.example.demo.Entity.Report.ReportStatus;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Report.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.demo.Entity.Report.ReportStatus.PENDING;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportViewService {

    private final ReportRepository reportRepository;

    /**
     *  [목록] 대기중(PENDING) 또는 완료(PROCESSED) 상태별 신고 리스트 조회
     */
    public Page<ReportResponse> getReportList(ReportStatus status, Pageable pageable) {
        return reportRepository.findByStatus(status, pageable)
                .map(ReportResponse::from); // 응답용 DTO 변환
    }

    /**
     *  [상세] 특정 신고 내역 상세 보기
     */
    public ReportDetailResponse getReportDetail(Long id) {
        Report report = reportRepository.findDetailById(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 신고 내역입니다."));
        return ReportDetailResponse.from(report);
    }

    /**
     *  신고 대기중인 데이터 개수
     */
    public long getPendingReportCount()
    {
        return reportRepository.countByStatus(PENDING);
    }

}
