package com.example.demo.Controller.Item;

import com.example.demo.DTO.Chat.Response.ChatBuyerResponse;
import com.example.demo.DTO.CommonResponse;
import com.example.demo.DTO.Users.Response.UserDetailResponse;
import com.example.demo.DTO.item.Request.ItemCreateRequest;
import com.example.demo.DTO.item.Request.ItemStatusUpdateRequest;
import com.example.demo.DTO.item.Request.ItemUpdateRequest;
import com.example.demo.DTO.item.Response.ItemDetailResponse;
import com.example.demo.Service.Chat.ChatRoomReadService;
import com.example.demo.Service.Item.ItemCommandService;
import com.example.demo.Service.Item.ItemLikeCommandService;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Users.UserReadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items")
@Slf4j
public class ItemApiController {

    private final ItemCommandService itemCommandService;
    private final ItemLikeCommandService itemLikeCommandService;
    private final ChatRoomReadService chatRoomReadService;


    // 게시글 생성
    @PostMapping(path = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<?>> createItemPost(
            @ModelAttribute @Valid ItemCreateRequest form,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        itemCommandService.createItem(form, userDetails.getUsername());

        return ResponseEntity.ok(new CommonResponse<>(true, "상품이 등록되었습니다.", null));
    }

    // 게시글 수정
    @PutMapping("/update/{id}") // 또는 @PatchMapping
    public ResponseEntity<CommonResponse<?>> edit(@PathVariable Long id,
                                                  @ModelAttribute @Valid ItemUpdateRequest form,
                                  @AuthenticationPrincipal UserDetails userDetails
                                 ,@RequestParam(value = "deleteImageIds", required = false)
                                                      List<Long> deleteImageIds) throws IOException {

        // 서비스에서 권한 체크와 수정을 한 번에 처리
        itemCommandService.updateItem(id, form, userDetails.getUsername(),deleteImageIds);

        return ResponseEntity.ok(new CommonResponse<>(true, "상품정보가 수정되었습니다.", null));
    }

    // 게시글 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<CommonResponse<?>> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails userDetails) {


        itemCommandService.deleteItem(id, userDetails.getUsername());

        return ResponseEntity.ok(new CommonResponse<>(true, "게시글이 성공적으로 삭제되었습니다.", null));
    }


    // 찜(좋아요) 기능
    @PostMapping("/vote/{id}")
    public ResponseEntity<CommonResponse<?>> BoardVote(@AuthenticationPrincipal UserDetails userDetails,
                                       @PathVariable("id") Long id) {


        itemLikeCommandService.toggleLike(id,userDetails.getUsername());
        return ResponseEntity.ok(new CommonResponse<>(true, "관심 상품 등록/취소가 처리되었습니다.", null));
    }

    // 판매 상태 변경
    @PatchMapping("/status/{id}")
    public ResponseEntity<CommonResponse<?>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ItemStatusUpdateRequest dto, // @Valid 추가
            @AuthenticationPrincipal UserDetails userDetails) {

        itemCommandService.updateStatus(id, dto.itemStatus(), userDetails.getUsername(), dto.buyerUsername());

        return ResponseEntity.ok(new CommonResponse<>(true, "판매 상태가 변경되었습니다.", null));
    }

    // 구매자 채팅방 찾기
    @GetMapping("/{itemId}/chat-buyers")
    public ResponseEntity<CommonResponse<List<ChatBuyerResponse>>> getChatBuyers(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ChatBuyerResponse> buyers = chatRoomReadService.getBuyersByItemId(itemId, userDetails.getUsername());

        return ResponseEntity.ok(new CommonResponse<>(true, "구매자 후보 목록을 조회했습니다.", buyers));
    }

}

