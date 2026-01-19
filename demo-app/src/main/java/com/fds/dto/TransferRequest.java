package com.fds.dto;

public record TransferRequest(
        String userId,
        Long amount
) {}
