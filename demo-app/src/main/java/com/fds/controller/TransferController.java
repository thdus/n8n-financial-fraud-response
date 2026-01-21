package com.fds.controller;

import com.fds.dto.TransferRequest;
import com.fds.service.TransferService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/api/transfer")
    public ResponseEntity<Map<String, Object>> transfer(
            @RequestParam String userId,
            @RequestParam Long amount,
            @RequestParam(required = false) String country,
            @RequestParam(required = false, defaultValue = "false") Boolean verified,
            HttpServletRequest request
    ) {
        log.info("Transfer request: userId={}, amount={}, country={}, verified={}",
                userId, amount, country, verified);

        Map<String, Object> result = transferService.transfer(userId, amount, country, verified, request);

        return ResponseEntity.ok(result);
    }

    // 기존 /transfer 엔드포인트 (혹시 다른 곳에서 쓰고 있다면)
    @PostMapping("/transfer")
    public String transferLegacy(
            @RequestBody TransferRequest req,
            HttpServletRequest httpRequest
    ) {
        transferService.transfer(
                req.userId(),
                req.amount(),
                req.country(),
                httpRequest
        );

        return "TRANSFER_REQUESTED";
    }
}