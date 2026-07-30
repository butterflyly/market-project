package com.example.demo.DTO.Alram;

import com.example.demo.Entity.Users.Users;

public record ReportProcessedEvent(
        Users reporter,
        String message
) {}