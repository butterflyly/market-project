package com.example.demo.Repository.Alram;

import com.example.demo.Entity.Alarm.Notification;
import com.example.demo.Entity.Users.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Arrays;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification , Long> {

    List<Notification> findByReceiverAndReadFalseOrderByCreatedAtDesc(Users receiver);

    // 1. 특정 유저의 읽지 않은 특정 채팅방 알림 존재 여부 확인
    boolean existsByReceiverAndTargetUrlAndReadFalse(Users receiver, String targetUrl);

    // 2. 특정 유저의 전체 안 읽은 알림 개수 조회
    long countByReceiverAndReadFalse(Users receiver);

}
