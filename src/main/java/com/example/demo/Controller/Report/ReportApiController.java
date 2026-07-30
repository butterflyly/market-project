package com.example.demo.Controller.Report;

import com.example.demo.DTO.CommonResponse;
import com.example.demo.DTO.Report.ReportRequest;
import com.example.demo.Service.Report.ReportCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportApiController {

    private final ReportCommandService reportService;

    //  /api/reports/item/5 번 주소로 비동기 POST 매핑 진행
    @PostMapping("/item/{itemId}")
    public ResponseEntity<CommonResponse<?>> reportItem(
            @PathVariable Long itemId,
            @Valid @RequestBody ReportRequest dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        reportService.createItemReport(itemId, dto, userDetails.getUsername());

        return ResponseEntity.ok(new CommonResponse<>(true, "신고가 접수되었습니다.", null));
    }
}