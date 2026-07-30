package com.example.demo.Config;

/*
@RequiredArgsConstructor
@Component
@Slf4j

 */
/*
  소프트 삭제 데이터 or 특정 시간만 존재하는 데이터를 기간을 설정하여 HARD DELETE 시키는 클래스
 */

import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Chat.ChatMessage.ChatMessageRepository;
import com.example.demo.Repository.Chat.ChatRoom.ChatRoomRepository;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Repository.Report.ReportRepository;
import com.example.demo.Repository.Review.ReviewRepository;
import com.example.demo.Repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class DeleteCleanUpScheduler {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     *  1. 만료 회원 완전 삭제 배치 (매일 새벽 0시)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void hardDeleteExpiredUsers() {
        log.info(" [시스템 정책] 탈퇴 후 한 달이 경과한 회원 완전 삭제 배치 시작");

        LocalDateTime beforeTime = LocalDateTime.now().minusMonths(1);
        List<Long> expiredUserIds = userRepository.findExpiredUserIdsForHardDelete(beforeTime);

        if (expiredUserIds.isEmpty()) {
            log.info(" [시스템 정책] 삭제 대상 만료 회원이 없습니다.");
            return;
        }

        try {

            // DB에 직접 주입해 둔 0번 고스트 유저 조회
            Long ghostUserId = 0L;
            Users ghostUser = userRepository.findById(ghostUserId)
                    .orElseThrow(() -> new BusinessException("시스템 유령 유저가 없습니다."));

            // [순서 1] 삭제 대상 유저의 소유 데이터(상품, 리뷰, 신고)를 고스트 유저로 이관
            itemRepository.transferSellerToGhost(expiredUserIds, ghostUser);
            reviewRepository.transferWriterToGhost(expiredUserIds, ghostUser);
            reviewRepository.transferReceiverToGhost(expiredUserIds, ghostUser);
            reportRepository.transferReporterToGhost(expiredUserIds, ghostUser);

            // [순서 2] 채팅 메시지의 sender 를 고스트 유저로 이관 (기존 닉네임 문자열 스냅샷은 그대로 보존됨)
            chatMessageRepository.transferSenderToGhost(expiredUserIds, ghostUser);

            // [순서 3] 탈퇴 만료 회원들의 채팅방 주인 유령 유저로 이관
            chatRoomRepository.transferBuyerToGhost(expiredUserIds, ghostUser);
            chatRoomRepository.transferSellerToGhost(expiredUserIds, ghostUser);

            // [순서 4] 양쪽 다 유령이 되어 더 이상 소통이 불가능한 방 완전 폭파
            chatRoomRepository.deletePureGhostRooms(ghostUserId);

            // [순서 5] 연관 끈이 완전히 정리된 만료 유저 본체 최종 하드 삭제 (IN 쿼리로 벌크 삭제)
            userRepository.deleteAllByIdInBatch(expiredUserIds);

            log.info(" [시스템 정책] 만료 회원 총 {}명 완전히 DB에서 소멸 및 데이터 이관 완료.", expiredUserIds.size());
        } catch (Exception e) {
            log.error(" [시스템 정책] 만료 회원 청소 중 예외 발생: ", e);
        }
    }

    /**
     *  2. 만료 상품 완전 삭제 배치 (매일 새벽 0시)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void hardDeleteExpiredItems() {
        log.info(" [시스템 정책] 삭제 후 한 달이 경과한 상품 게시글 완전 삭제 배치 시작");

        LocalDateTime beforeTime = LocalDateTime.now().minusMonths(1);
        List<Long> expiredItemIds = itemRepository.findIdsByDeletedTrueAndUpdatedAtBefore(beforeTime);

        if (expiredItemIds.isEmpty()) {
            log.info(" [시스템 정책] 삭제 대상 만료 상품이 없습니다.");
            return;
        }

        try {
            //  [순서 1] 상품을 진짜 DB에서 지우기 전에, 해당 상품을 물고 있던 신고(Report)의 연관관계를 null 로 해제
            reportRepository.updateReportedItemToNull(expiredItemIds);

            //  [순서 2] 상품 물리 삭제 진행 (벌크 한방 쿼리)
            itemRepository.deleteAllByIdsIn(expiredItemIds);

            log.info(" [시스템 정책] 만료 상품 총 {}건이 DB에서 영구 삭제되었습니다.", expiredItemIds.size());
        } catch (Exception e) {
            log.error(" [시스템 정책] 만료 상품 청소 중 예외 발생: ", e);
        }
    }
}

