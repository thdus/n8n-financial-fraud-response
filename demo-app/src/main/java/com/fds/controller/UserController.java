package com.fds.controller;

import com.fds.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    // n8n에서 위험도 분석 결과 받기
    @PostMapping("/update-risk")
    public Map<String, String> updateRiskStatus(@RequestBody Map<String, String> request) {
        String userId = request.get("user_id");
        String riskLevel = request.get("risk_level"); // "LOW", "MEDIUM", "HIGH"

        switch (riskLevel.toUpperCase()) {
            case "HIGH":
                // HIGH: 자동 차단
                AuthService.updateUserBlocked(userId, true);
                log.warn("RISK_UPDATE userId={} riskLevel=HIGH blocked=true", userId);

                return Map.of(
                        "status", "success",
                        "user_id", userId,
                        "risk_level", "HIGH",
                        "blocked", "true",
                        "message", "User automatically blocked due to high risk"
                );

            case "MEDIUM":
                // MEDIUM: 추가 인증 필요 (차단은 안 함)
                log.warn("RISK_UPDATE userId={} riskLevel=MEDIUM (verification required)", userId);

                return Map.of(
                        "status", "success",
                        "user_id", userId,
                        "risk_level", "MEDIUM",
                        "blocked", "false",
                        "message", "Additional verification required"
                );

            case "LOW":
            default:
                // LOW: 정상
                log.info("RISK_UPDATE userId={} riskLevel=LOW", userId);

                return Map.of(
                        "status", "success",
                        "user_id", userId,
                        "risk_level", "LOW",
                        "blocked", "false",
                        "message", "Normal status"
                );
        }
    }

    // 관리자가 수동으로 차단
    @PostMapping("/block")
    public Map<String, String> blockUser(@RequestBody Map<String, String> request) {
        String userId = request.get("user_id");

        AuthService.updateUserBlocked(userId, true);
        log.warn("USER_BLOCKED userId={} by_admin=true", userId);

        return Map.of(
                "status", "success",
                "message", "User blocked by admin: " + userId,
                "blocked", "true"
        );
    }

    // 관리자가 차단 해제 (오탐 처리)
    @PostMapping("/unblock")
    public Map<String, String> unblockUser(@RequestBody Map<String, String> request) {
        String userId = request.get("user_id");

        AuthService.updateUserBlocked(userId, false);
        log.info("USER_UNBLOCKED userId={} by_admin=true", userId);

        return Map.of(
                "status", "success",
                "message", "User unblocked by admin: " + userId,
                "blocked", "false"
        );
    }

    // 사용자 상태 조회
    @GetMapping("/status/{userId}")
    public Map<String, Object> getUserStatus(@PathVariable String userId) {
        Boolean blocked = AuthService.getUserBlocked(userId);

        return Map.of(
                "userId", userId,
                "blocked", blocked
        );
    }
}