package com.fds.dto;

import java.time.LocalDateTime;

public record FdsEvent(
        String eventType,
        String userId,
        Long amount,
        String loginIp,
        String requestIp,
        String userAgent,
        LocalDateTime timestamp
) {}
