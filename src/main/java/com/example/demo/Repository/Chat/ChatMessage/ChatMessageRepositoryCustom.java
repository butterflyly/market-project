package com.example.demo.Repository.Chat.ChatMessage;


import com.example.demo.Entity.Users.Users;

import java.util.List;

public interface ChatMessageRepositoryCustom {

    void markAsReadWithQuerydsl(Long roomId, String username);

    void transferSenderToGhost(List<Long> expiredUserIds, Users ghostUser);
}
