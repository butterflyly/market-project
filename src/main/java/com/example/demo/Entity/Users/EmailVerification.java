package com.example.demo.Entity.Users;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class EmailVerification {

    @Id
    @GeneratedValue
    private Long id;

    private String email;

    private String code;

    private LocalDateTime expireTime;

    private boolean verified;
}