package com.example.demo.Service.Report;

import com.example.demo.Config.NotificationEventListener;
import com.example.demo.DTO.Alram.ReportProcessedEvent;
import com.example.demo.DTO.Alram.UserWarningEvent;
import com.example.demo.DTO.Report.ReportRequest;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Report.Report;
import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Report.ReportRepository;
import com.example.demo.Service.Alram.NotificationService;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Users.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.demo.Entity.Alarm.NotificationType.SYSTEM;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportCommandService {

    private final ReportRepository reportRepository;
    private final ItemReadService itemReadService;
    private final UserReadService userReadService;
    private final ApplicationEventPublisher eventPublisher;

    public void createItemReport(Long itemId, ReportRequest dto, String loginUsername) {

        // 1. 신고자와 대상 게시글 긁어오기
       Users reporter = userReadService.getUserEntity(loginUsername);

        Item item = itemReadService.getItemEntity(itemId);

        // 2. 피신고자(판매자) 특정하기
        Users reportedUser = item.getSeller();

        // 3. 기획 예외 조건 체크: 자기 글을 자기가 저격 불가
        if (reporter.getUsername().equals(reportedUser.getUsername())) {
            throw new BusinessException("본인이 작성한 글은 신고할 수 없습니다.");
        }

        // 4. 빌더로 영속성 객체 싹 조립해서 한 방에 밀어넣기
        Report report = Report.builder()
                .reporter(reporter)
                .reportedUser(reportedUser)
                .reportedItem(item)
                .reportType(dto.reportType())
                .content(dto.content())
                .build();

        reportRepository.save(report);
    }

    /**
     *  1. 사기꾼 즉시 처단 액션 (글 삭제 + 유저 즉시 블랙 + 신고 종결)
     * 사기(SCAM) 건은 경고 누적을 기다리지 않고 그 즉시 최악의 제재를 가합니다.
     */
    public void executeScamSanction(Long reportId) {

        Report report = findReportWithDetail(reportId);
        Item item = validateAndGetReportedItem(report);

        // 1. 게시글 삭제 및 유저 블랙 처리
        item.delete();
        Users seller = item.getSeller();
        seller.forceBlack();
        seller.receiveWarning();

        // 2. 신고 건 종결
        report.completeProcessing();

        // 3-1. 신고자 대상 처리 완료 알림 발행
        String reporterMsg = "신고해주신 유저에 대한 처리가 완료되었습니다.\n해당 유저는 경고 처리 되었습니다.";
        eventPublisher.publishEvent(new ReportProcessedEvent(report.getReporter(), reporterMsg));

        // 3-2.  피신고자(경고 수신 유저) 대상 경고 알림 발행
        eventPublisher.publishEvent(new UserWarningEvent(
                seller,
                seller.getWarningCount(), // 유저 엔티티의 누적 경고 수 Getter
                seller.getRole() == UserRole.Black // 블랙 상태 전환 여부
        ));
    }

    /**
     * ️ 2. 정책 기반 경고 부여 + 게시글 블라인드 액션 (경고 무조건 누적 + 3진아웃 반영)
     * 광고(SPAM)나 금지물품(PROHIBITED_ITEM) 등 일반적인 위반 사항에 사용됩니다.
     */
    public void blindItemWithWarning(Long reportId) {

        Report report = findReportWithDetail(reportId);
        Item item = validateAndGetReportedItem(report);

        // 1. 게시글 삭제 및 경고 부여
        item.delete();
        item.getSeller().receiveWarning();

        // 2. 신고 건 종결
        report.completeProcessing();

        // 3. 알림 이벤트 발행
        String message = "신고해주신 유저에 대한 처리가 완료되었습니다.\n해당 유저는 경고 처리 되었습니다.";
        eventPublisher.publishEvent(new ReportProcessedEvent(report.getReporter(), message));
    }

    /**
     *  3. 무혐의 단순 종결 액션 (글 삭제 없음, 제재 없음)
     * 허위 신고나 악의적 신고일 경우, 아무 조치 없이 신고 건만 닫아버립니다.
     */
    public void dismissReport(Long reportId) {
        Report report = findReportWithDetail(reportId);

        // 1. 신고 상태만 처리 완료로 변경
        report.completeProcessing();

        // 2. 알림 이벤트 발행
        String message = "신고해주신 유저에 대한 처리가 완료되었습니다.\n해당 유저는 무혐의 처리되었습니다.";
        eventPublisher.publishEvent(new ReportProcessedEvent(report.getReporter(), message));
    }


    /**
     * 신고 엔티티 단건 조회 및 검증
     */
    private Report findReportWithDetail(Long reportId) {
        return reportRepository.findDetailById(reportId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 신고입니다. ID: " + reportId));
    }

    /**
     * 신고된 게시글 검증 및 추출
     */
    private Item validateAndGetReportedItem(Report report) {
        Item item = report.getReportedItem();
        if (item == null) {
            throw new BusinessException("신고된 게시글 정보가 존재하지 않습니다.");
        }
        return item;
    }
}