package com.fds.controller;

import com.fds.service.GoogleSheetsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final GoogleSheetsService googleSheetsService;

    // n8n에서 위험도 분석 결과 받기
    @PostMapping("/update-risk")
    public Map<String, String> updateRiskStatus(@RequestBody Map<String, String> request) {
        String userId = request.get("user_id");
        String riskLevel = request.get("risk_level");

        switch (riskLevel.toUpperCase()) {
            case "HIGH":
                // HIGH: Google Sheets에 blocked=TRUE 설정
                googleSheetsService.blockUser(userId);
                log.warn("RISK_UPDATE userId={} riskLevel=HIGH blocked=true", userId);

                return Map.of(
                        "status", "success",
                        "user_id", userId,
                        "risk_level", "HIGH",
                        "blocked", "true",
                        "message", "User automatically blocked due to high risk"
                );

            case "MEDIUM":
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

}