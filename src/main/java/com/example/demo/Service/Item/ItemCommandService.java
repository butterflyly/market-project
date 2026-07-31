package com.example.demo.Service.Item;

import com.example.demo.DTO.Alram.DealConfirmedEvent;
import com.example.demo.DTO.Alram.ItemStatusChangedEvent;
import com.example.demo.DTO.item.Request.ItemCreateRequest;
import com.example.demo.DTO.item.Request.ItemUpdateRequest;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemCategory;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Item.ItemLike.ItemLikeRepository;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Service.Users.UserReadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemCommandService {

    private final ItemRepository itemRepository;
    private final UserReadService userReadService;
    private final ItemCategoryService itemCategoryService;
    private final ItemImageService itemImageService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ApplicationEventPublisher eventPublisher; // 스프링 내장 확성기


    private static final int MAX_IMAGES = 5;


    /**
     *  Command 서비스 전용 엔티티 조회 및 예외 처리 메서드
     */
    private Item findItem(Long id) {
        return itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("존재하지 않거나 이미 삭제된 상품입니다."));
    }

    /**
     * 게시글 작성
     */
    public void createItem(ItemCreateRequest form, String username) throws IOException {

        Users users = userReadService.getUserEntity(username);

        if (users.getWarningCount() >= 2) {
            throw new BusinessException("경고 2회 이상 누적으로 인해 현재 게시글 작성이 제한된 상태입니다.");
        }

        if (form.images() != null && form.images().size() > MAX_IMAGES) {
            throw new IllegalArgumentException(
                    "이미지는 최대 " + MAX_IMAGES + "장까지 업로드할 수 있습니다."
            );
        }

        ItemCategory category = itemCategoryService.findByCategory(form.categoryId());

        Item item = Item.builder().title(form.title())
                .content(form.content())
                .seller(users)
                .itemCategory(category)
                .price(form.price())
                .itemStatus(ItemStatus.ON_SALE)
                .build();

        itemRepository.save(item);

        // 이미지 엔티티 및 실제 데이터 저장
        itemImageService.saveImages(item,form.images());

    }

    /**
     * 게시글 수정
     */
    public void updateItem(Long id, @Valid ItemUpdateRequest form, String username, List<Long> deleteImageIds) throws IOException {

        Item item = findItem(id);

        if(!item.getSeller().getUsername().equals(username))
        {
            throw new BusinessException("해당 게시글을 수정할 권한이 없습니다.");
        }

        // 삭제 이미지 유효성
        itemImageService.validateDeleteImages(item, deleteImageIds);

        // 이미지 개수 검증
        itemImageService.validateImageCount(item, form.images(), deleteImageIds);

        ItemCategory itemCategory = itemCategoryService.findByCategory(form.categoryId());

        item.update(form.title(),form.content(), form.price(), itemCategory);

        itemRepository.save(item);

        // 이미지 삭제
        itemImageService.deleteImages(item, deleteImageIds);

        // 이미지 추가
        itemImageService.saveImages(item, form.images());

    }

    public void deleteItem(Long id, String username) {

        Item item = findItem(id);

        // 본인 확인
        if (!item.getSeller().getUsername().equals(username)) {
            throw new BusinessException("삭제 권한이 없습니다.");
        }

        // 실제 삭제 대신 상태 변경 (Soft Delete)
        item.delete();
    }

/* =======================================================================
 * 쿠키(Cookie) 기반 조회수 중복 방지 로직
 * * - 장점: 별도의 서버 저장소(DB, 캐시) 없이 클라이언트 브라우저에 데이터를 저장하므로 가볍다.
 * - 단점: 사용자가 많아지거나 글을 많이 읽으면 쿠키 문자열이 너무 길어져 네트워크 낭비가 발생함.
 * 사용자가 쿠키를 삭제하거나 시크릿 모드를 쓰면 쉽게 우회됨.
 * =======================================================================

 public void updateViewsWithCookie(Long id, HttpServletRequest request, HttpServletResponse response) {
     Cookie oldCookie = null;
     Cookie[] cookies = request.getCookies(); // 1. 브라우저가 보낸 쿠키 목록을 가져옴

     // 2. 쿠키 목록 중 우리가 조회수 체크용으로 만든 "itemViews"가 있는지 확인
     if (cookies != null) {
         for (Cookie cookie : cookies) {
             if (cookie.getName().equals("itemViews")) {
                 oldCookie = cookie; // 찾았다면 해당 쿠키를 저장
                 break;
             }
         }
     }

     String targetValue = "[" + id + "]"; // 중복 체크를 위한 문자열 포맷 (예: [12])

     // 3. 기존에 발급된 쿠키가 있는 경우
     if (oldCookie != null) {
         // 쿠키 값 내에 현재 상품 ID([12])가 포함되어 있지 않다면 (즉, 오늘 처음 보는 글이라면)
         if (!oldCookie.getValue().contains(targetValue)) {
             itemRepository.updateView(id); // DB 조회수 증가 쿼리 실행

             // 기존 쿠키 값 뒤에 새로운 상품 ID를 이어 붙임 (예: [5]_[12])
             oldCookie.setValue(oldCookie.getValue() + "_" + targetValue);
             oldCookie.setPath("/");               // 모든 경로에서 이 쿠키를 서버로 전송하도록 설정
             oldCookie.setMaxAge(60 * 60 * 24);    // 쿠키 수명 설정 (24시간)
             response.addCookie(oldCookie);        // 변경된 쿠키를 다시 브라우저에 배달
         }
     }
     // 4. 오늘 아예 처음으로 아무 글이나 읽어서 쿠키 자체가 없는 경우
     else {
         itemRepository.updateView(id); // 즉시 DB 조회수 증가

         // 새로운 조회수 쿠키 생성 (이름: "itemViews", 값: "[12]")
         Cookie newCookie = new Cookie("itemViews", targetValue);
         newCookie.setPath("/");
         newCookie.setMaxAge(60 * 60 * 24);       // 쿠키 수명 설정 (24시간)
         response.addCookie(newCookie);           // 생성한 쿠키를 브라우저에 저장하도록 전달
     }
 }
 ======================================================================= */

    // 조회수 로직
    public void updateViews(Long id, HttpServletRequest request) {

        // 1. 유저의 IP 주소 가져오기
        String clientIp = getClientIp(request);

        // 2. Redis Key 생성 (예: item:views:12)
        String redisKey = "item:views:" + id;

        // 3. Redis Set에 IP 추가 시도
        // SADD 명령어는 데이터가 처음 들어갈 때만 1L을 반환합니다.
        Long addedCount = redisTemplate.opsForSet().add(redisKey, clientIp);

        // 4. 처음 방문한 IP라면 DB 조회수 증가 및 만료 시간 설정
        if (addedCount != null && addedCount == 1L) {
            itemRepository.updateView(id); // DB 조회수 1 증가 쿼리 실행

            // 이 기록(Set)이 하루(24시간) 동안만 유지되도록 설정
            redisTemplate.expire(redisKey, Duration.ofDays(1));
        }
    }

    /**
     * 프록시 서버나 로드 밸런서를 거쳐도 실제 클라이언트 IP를 정확히 가져오는 메서드
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr(); // 가장 기본적인 IP 가져오기
        }
        return ip;
    }


    // 판매상태 변경
    public void updateStatus(Long id, ItemStatus status, String username,String buyerUsername) {

        Item item = findItem(id);

        if (!item.getSeller().getUsername().equals(username)) {
            throw new BusinessException("판매상태를 변경할 권한이 없습니다");
        }

        //  예약중 -> 판매완료로 변경할 때 buyerUsername 이 누락되었다면 기존 예약자 id를 자동으로 매핑
        if (item.getItemStatus() == ItemStatus.RESERVED && status == ItemStatus.SOLD_OUT) {
            if (buyerUsername == null || buyerUsername.isBlank()) {
                if (item.getBuyer() != null) {
                    buyerUsername = item.getBuyer().getUsername();
                }
            } else {
                // 외부 유저가 다르게 넘어온 경우 제약 조건 방어
                if (item.getBuyer() != null && !item.getBuyer().getUsername().equals(buyerUsername)) {
                    throw new IllegalStateException("현재 예약된 구매자와 판매완료 처리하려는 구매자가 일치하지 않습니다.");
                }
            }
        }

        // 1. 만약 판매완료(SOLD_OUT) 또는 예약중(RESERVED) 이라면 구매자 엔티티를 조회
        Users buyer = null;
        if (status == ItemStatus.SOLD_OUT || status == ItemStatus.RESERVED) {
            if (buyerUsername == null || buyerUsername.isBlank()) {
                throw new BusinessException(status.name() + " 처리 시 대상자 지정은 필수입니다.");
            }
            buyer = userReadService.getUserEntity(buyerUsername);
        }

        // 2. 엔티티에 바뀐 상태와 구매자를 함께
        item.changeStatus(status, buyer);

        // 3. 상태 변경에 따른 알림 이벤트 발행
        String targetUrl = "/items/detail/" + item.getId();

        // [이벤트 1] 찜한 유저 전체 대상 - 상태 변경 알림 (RESERVED, SOLD_OUT, SELLING 모두 발송)
        eventPublisher.publishEvent(new ItemStatusChangedEvent(item, targetUrl));

        // [이벤트 2] 거래 완료(SOLD_OUT) 시 구매자 대상 - 거래 확정(리뷰 작성 유도) 알림 추가 발송
        if (status == ItemStatus.SOLD_OUT && buyer != null) {
            eventPublisher.publishEvent(new DealConfirmedEvent(
                    buyer,                               // 구매자 엔티티 (또는 buyer.getId())
                    item.getSeller().getDisplayNickname(),// 판매자 닉네임
                    item.getTitle(),                     // 상품 제목
                    targetUrl
            ));
        }
    }

}
