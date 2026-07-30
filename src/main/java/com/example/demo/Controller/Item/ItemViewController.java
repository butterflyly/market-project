package com.example.demo.Controller.Item;

import com.example.demo.DTO.Chat.Response.ChatBuyerResponse;
import com.example.demo.DTO.CommonResponse;
import com.example.demo.DTO.item.Request.ItemCreateRequest;
import com.example.demo.DTO.item.Response.ItemDetailResponse;
import com.example.demo.DTO.item.Response.ItemSummaryResponse;
import com.example.demo.Entity.Item.ItemCategory;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Error.BusinessException;
import com.example.demo.Service.Chat.ChatRoomReadService;
import com.example.demo.Service.Item.ItemCategoryService;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Users.UserReadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/items")
public class ItemViewController {

    private final ItemReadService itemReadService;
    private final ItemCategoryService itemCategoryService;
    private final UserReadService userReadService;

    @GetMapping("/create")
    public String itemCreate(@RequestParam(value = "categoryId", required = false) Long categoryId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model)
    {

        userReadService.validateItemCreatePermission(userDetails.getUsername());

        List<ItemCategory> categories = itemCategoryService.findAll();

        model.addAttribute("categories", categories);

        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("itemCreateForm", new ItemCreateRequest("", 0, "", null,null));

        return "item/create";
    }

    @GetMapping("/list/{categoryId}")
    public String list(@PathVariable Long categoryId,
                       Model model,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "latest") String sort,
                       @RequestParam(value = "status",defaultValue = "ON_SALE") ItemStatus status,
                       @RequestParam(value = "kw", defaultValue = "") String kw // 검색
            , @RequestParam(value = "searchType", defaultValue = "title") String type)
    {
        model.addAttribute("categories", itemCategoryService.findAll());

        //  1. 프론트 정렬 키워드("latest", "views" 등)를 JPA Sort 객체로 변환
        Sort sortOption = getSortOption(sort);

        //  2. Pageable 객체 완성 (20개씩 페이징 + 정렬 포함)
        Pageable pageable = PageRequest.of(page, 20, sortOption);

        //  3. 완성된 pageable 만 깔끔하게 서비스로 전달
        Page<ItemSummaryResponse> itemList = itemReadService.getList(kw, categoryId, pageable, type);

        model.addAttribute("items", itemList);
        model.addAttribute("currentCategoryId", categoryId);

        if (categoryId != null) {
            ItemCategory category = itemCategoryService.findByCategory(categoryId);
            model.addAttribute("categoryName", category.getName());
        }

        return "item/list";
    }

    private Sort getSortOption(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));
        }

        return switch (sort) {
            case "views" -> Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by(Sort.Direction.DESC, "id"));
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")); // "latest"
        };
    }

    @GetMapping("/detail/{id}")
    public String itemDetail(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest httpServletRequest)
    {
        String username = (userDetails != null) ? userDetails.getUsername() : "";

        // 2. 서비스 호출 및 모델 담기
        // 상세 조회 DTO(ItemDetailResponse)에는 title, content, isOwner 등이 포함됨
        ItemDetailResponse detail = itemReadService.getDetailWithViews(id, username,httpServletRequest);
        model.addAttribute("item", detail);

        return "item/detail";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {

        ItemDetailResponse detail = itemReadService.getDetail(id, userDetails.getUsername());

        if (!detail.isOwner()) {
            throw new BusinessException("본인이 작성한 글만 수정할 수 있습니다.");
        }

        model.addAttribute("item", detail);
        model.addAttribute("categories", itemCategoryService.findAll());

        return "item/edit";
    }

}
