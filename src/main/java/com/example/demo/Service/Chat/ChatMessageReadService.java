package com.example.demo.Service.Chat;

import com.example.demo.DTO.Chat.Response.ChatMessageResponse;
import com.example.demo.Entity.Chat.ChatMessage;
import com.example.demo.Repository.Chat.ChatMessage.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageReadService {

    private final ChatMessageRepository chatMessageRepository;

    public List<ChatMessageResponse> getMessages(Long roomId, String loginUsername) {

        // 0페이지에서 20개만, 생성일자 내림차순(최신순)으로 요청
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

        Slice<ChatMessage> messageSlice = chatMessageRepository.findByChatRoomId(roomId, pageable);

        return messageSlice.getContent().stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt)) // 사용자 화면을 위해 오름차순 정렬
                .map(m -> ChatMessageResponse.of(m, loginUsername))
                .collect(Collectors.toList());
    }
}
