package com.example.demo.Controller.Chat;

import com.example.demo.DTO.Chat.ChatRoomCreateResult;
import com.example.demo.DTO.Chat.Request.ChatRoomCreateRequest;
import com.example.demo.DTO.CommonResponse;
import com.example.demo.Service.Chat.ChatRoomCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-room")
public class ChatRoomApiController {

    private final ChatRoomCommandService chatRoomCommandService;


    @PostMapping("/create")
    public ResponseEntity<CommonResponse<Long>> createChatRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid ChatRoomCreateRequest request) {

        // chatRoomCreate 서비스로 itemId와 로그인 유저 네임을 전달
        ChatRoomCreateResult chatRoomCreateResult =
                chatRoomCommandService.chatRoomCreate(request.itemId(), userDetails.getUsername());

        return ResponseEntity.ok(new CommonResponse<>(
                true,
                chatRoomCreateResult.message(),
                chatRoomCreateResult.roomId()
        ));
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<CommonResponse<Long>> leaveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        chatRoomCommandService.leaveRoom(roomId, userDetails.getUsername());

        return ResponseEntity.ok(new CommonResponse<>(true,"채팅방에서 성공적으로 퇴장했습니다.", roomId));
    }
}
