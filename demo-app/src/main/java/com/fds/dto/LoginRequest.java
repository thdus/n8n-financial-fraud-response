package com.fds.dto;

public record LoginRequest(
        String userId,
        String password,
        String country
) {}

