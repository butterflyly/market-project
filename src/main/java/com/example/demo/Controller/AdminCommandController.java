package com.example.demo.Controller;

import com.example.demo.DTO.CommonResponse;
import com.example.demo.Service.Item.ItemCommandService;
import com.example.demo.Service.Report.ReportCommandService;
import com.example.demo.Service.Users.UserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminCommandController {

    private final ReportCommandService reportService;
    private final UserCommandService userCommandService;
    private final ItemCommandService itemCommandService;

    /**
     *  액션 1: 사기꾼 핵버튼 (자바 전용 뷰 리다이렉트 버전)
     */
    @PostMapping("/reports/execute-scam/{id}")
    public String executeScam(@PathVariable Long id) {

        // 1. 백엔드 서비스 로직 완벽 실행
        reportService.executeScamSanction(id);

        // 2. 원래 신고 상세 페이지로 강제 새로고침(리다이렉트)
        return "redirect:/admin/reports/detail/" + id;
    }

    /**
     *  액션 2: 일반 제재
     */
    @PostMapping("/reports/blind-item/{id}")
    public String blindItem(@PathVariable Long id) {
        reportService.blindItemWithWarning(id);
        return "redirect:/admin/reports/detail/" + id;
    }

    /**
     *  액션 3: 무혐의 처리
     */
    @PostMapping("/reports/process/{id}")
    public String processReport(@PathVariable Long id) {
        reportService.dismissReport(id);
        return "redirect:/admin/reports/detail/" + id;
    }

    /**
     * 임의 블랙 조치
     */
    @PostMapping("/users/force-black/{id}")
    public String forceBlack(@PathVariable Long id) {
        userCommandService.forceBlackUser(id);
        return "redirect:/admin/users";
    }

    /**
     * 블랙 해제 (정상 회원 복원)
     */
    @PostMapping("/users/restore-black/{id}")
    public String restoreBlack(@PathVariable Long id) {
        userCommandService.restoreBlackUser(id);
        return "redirect:/admin/users";
    }

    /**
     * 소프트 삭제(탈퇴) 회원 복구
     */
    @PostMapping("/users/restore-deleted/{id}")
    public String restoreDeleted(@PathVariable Long id) {
        userCommandService.restoreDeletedUser(id);
        return "redirect:/admin/users";
    }

    /**
     *  유저 영구 삭제 제동(보존/해제) 토글
     */
    @PostMapping("/users/toggle-preserve/{id}")
    public String toggleUserPreserve(@PathVariable Long id) {
        userCommandService.toggleUserPreservation(id);
        return "redirect:/admin/users/detail/" + id; // 처리 후 상세 페이지로 리다이렉트
    }

    /**
     * 게시글 직접 보고 작성자 경고 1회 부여 (+ 해당 게시글 즉시 숨김)
     */
    @PostMapping("/items/{itemId}/warn-seller")
    public String warnSellerByItem(@PathVariable Long itemId) {
        // 1. 해당 게시글 작성자 경고 +1 및 게시글 처리
        userCommandService.warnSellerByItem(itemId);

        return "redirect:/items/list/2";
    }

    /**
     *  게시글 직접 보고 작성자 즉시 영구 정지(블랙) (+ 해당 게시글 즉시 숨김)
     */
    @PostMapping("/items/{itemId}/black-seller")
    public String forceBlackSellerByItem(@PathVariable Long itemId) {
        // 1. 해당 게시글 작성자 즉시 블랙 + 게시글 처리
        userCommandService.forceBlackSellerByItem(itemId);

        return "redirect:/items/list/2";
    }

}
