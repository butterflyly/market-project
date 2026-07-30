package com.example.demo.Controller;

import com.example.demo.DTO.Report.ReportDetailResponse;
import com.example.demo.DTO.Report.ReportResponse;
import com.example.demo.DTO.Users.Response.AdminUserDetailResponse;
import com.example.demo.DTO.Users.Response.AdminUserResponse;
import com.example.demo.DTO.item.Response.ItemDetailResponse;
import com.example.demo.DTO.item.Response.ItemSummaryResponse;
import com.example.demo.Entity.Report.ReportStatus;
import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Report.ReportViewService;
import com.example.demo.Service.Users.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminViewController {

    private final ItemReadService itemReadService;
    private final ReportViewService  reportViewService;
    private final UserReadService userReadService;


    /**
     *   통합 메인 대시보드 (홈 화면)
     */
    @GetMapping({"", "/dashboard"}) // /admin 또는 /admin/dashboard 둘 다 매핑
    public String adminHome(Model model) {

        model.addAttribute("pendingReportCount", reportViewService.getPendingReportCount());
        model.addAttribute("deletedItemCount", itemReadService.getDeletedItemCount());
        model.addAttribute("blackUserCount", userReadService.getBlackUserCount());

        return "admin/dashboard";
    }

    /**
     *  소프트 삭제된 게시글 통합 아카이브 조회
     */
    @GetMapping("/items/deleted-list")
    public String getDeletedItems(
            @RequestParam(value = "kw", defaultValue = "") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {


        Pageable pageable = PageRequest.of(page, 15, Sort.by("updatedAt").descending());

        // 서비스 호출 (아까 만들어둔 서비스 로직 그대로 활용 가능!)
        Page<ItemSummaryResponse> deletedItems =
                itemReadService.getDeletedItemsForAdmin(keyword, pageable);

        // 타임리프로 배달
        model.addAttribute("items", deletedItems);
        model.addAttribute("keyword", keyword); // 검색창에 입력했던 단어 유지용

        return "admin/deleted-items";
    }

    /**
     * 소프트 삭제된 게시글 상세 내역 확인 뷰
     */
    @GetMapping("/items/deleted-detail/{id}")
    public String getDeletedItemDetail(@PathVariable Long id, Model model) {

        // 1. 삭제된 글 상세 정보 + 작성자 정보 통째로 겟
        ItemDetailResponse deletedItem = itemReadService.getDeletedItemDetailForAdmin(id);

        // 2. 타임리프 템플릿에 배달
        model.addAttribute("item", deletedItem);

        return "admin/deleted-detail";
    }

    /**
     *  접수된 신고 목록 보기
     */
    @GetMapping("/reports")
    public String getReports(
            @RequestParam(value = "status", defaultValue = "PENDING") ReportStatus status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        // 최신 신고 등록 순으로 15개씩 페이징
        Pageable pageable = PageRequest.of(page, 15, Sort.by("createdAt").descending());

        Page<ReportResponse> reportPage = reportViewService.getReportList(status, pageable);

        model.addAttribute("reports", reportPage);
        model.addAttribute("currentStatus", status); // 탭 전환 UI 렌더링용

        return "admin/report-list";
    }

    /**
     *   특정 신고 내용 상세 보기
     */
    @GetMapping("/reports/detail/{id}")
    public String getReportDetail(@PathVariable Long id, Model model) {

        ReportDetailResponse reportDetail = reportViewService.getReportDetail(id);
        model.addAttribute("report", reportDetail);

        return "admin/report-detail"; // templates/admin/report-detail.html
    }

    /**
     * . 유저 관리 대시보드 (선량한 유저 리스트는 숨기고, 검색 혹은 블랙 유저 위주 모니터링)
     */
    @GetMapping("/users")
    public String userDashboard(
            @RequestParam(value = "tab", defaultValue = "ALL") String tab,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(page, 15, Sort.by("id").descending());
        Page<AdminUserResponse> userPage = userReadService.getTargetUserList(tab, keyword, pageable);

        model.addAttribute("users", userPage);
        model.addAttribute("currentTab", tab.toUpperCase());
        model.addAttribute("keyword", keyword);

        return "admin/user-management-list";
    }

    /**
     *  [팝업/상세] 유저 상세 요약 페이지
     */
    @GetMapping("/users/detail/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        AdminUserDetailResponse userDetail = userReadService.getUserDetail(id);
        model.addAttribute("user", userDetail);
        return "admin/user-detail-summary";
    }
}
