package com.fds.service;

import com.fds.dto.FdsEvent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_VERIFICATION_REQUIRED = "VERIFICATION_REQUIRED";
    private static final String RESULT_BLOCKED = "BLOCKED";
    private static final String SAMPLE_TO_BANK = "Woori";
    private static final String SAMPLE_TO_ACCOUNT_ID = "110-***-1234";
    private static final Map<String, String> COUNTRY_IP_MAP = Map.of(
            "KR", "203.0.113.10",
            "US", "198.51.100.23",
            "JP", "192.0.2.44",
            "SG", "203.0.113.77",
            "GB", "198.51.100.88"
    );

    private final EventSender eventSender;
    private final StringRedisTemplate redisTemplate;
    private final GoogleSheetsService googleSheetsService;

    // 기존 메소드 - 내부적으로 새 메소드 호출
    public void transfer(String userId, Long amount, String country, HttpServletRequest request) {
        transfer(userId, amount, country, false, request);
    }

    // 새 메소드 - verified 파라미터 추가
    public Map<String, Object> transfer(String userId, Long amount, String country, Boolean verified, HttpServletRequest request) {
        ZonedDateTime now = ZonedDateTime.now();
        String normalizedCountry = normalizeCountry(country);
        String srcIp = getClientIp(request, normalizedCountry);

        // Redis 카운트 증가 로직
        String redisKey = "tx_count:" + userId;
        Long txCount = redisTemplate.opsForValue().increment(redisKey);

        if (txCount != null && txCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(10));
            log.info("Redis key created: {} with TTL 10 minutes", redisKey);
        }
        log.info("User {} transfer count in 10min: {}", userId, txCount);

        // 1. 먼저 blocked 상태 체크 (관리자가 설정한 상태)
        Boolean isBlocked = AuthService.getUserBlocked(userId);
        if (isBlocked) {
            log.warn("TRANSFER_BLOCKED userId={} amount={} reason=ACCOUNT_BLOCKED", userId, amount);

            FdsEvent event = new FdsEvent(
                    now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    "TRANSFER",
                    UUID.randomUUID().toString(),
                    userId,
                    RESULT_BLOCKED,
                    srcIp,
                    normalizedCountry,
                    now.getHour(),
                    amount,
                    SAMPLE_TO_BANK,
                    SAMPLE_TO_ACCOUNT_ID
            );
            eventSender.send(event);

            return Map.of(
                    "status", RESULT_BLOCKED,
                    "message", "의심스러운 활동으로 인해 계정이 차단되었습니다.",
                    "amount", amount,
                    "toBank", SAMPLE_TO_BANK
            );
        }

        // 2. verified=true면 추가 인증 완료로 간주하고 위험도 체크 스킵
        String riskLevel;
        if (Boolean.TRUE.equals(verified)) {
            log.info("VERIFIED_TRANSFER userId={} amount={} - bypassing risk check", userId, amount);
            riskLevel = "VERIFIED";
        } else {
            // 2-1. Risk Level 조회 (실시간 위험도 체크)
            riskLevel = getRiskLevel(userId);

            // HIGH: 자동으로 blocked 상태로 변경
            if ("HIGH".equals(riskLevel)) {
                log.warn("TRANSFER_AUTO_BLOCKED userId={} amount={} riskLevel=HIGH (auto-blocking)", userId, amount);

                // User를 blocked 상태로 변경
                AuthService.updateUserBlocked(userId, true);

                FdsEvent event = new FdsEvent(
                        now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                        "TRANSFER",
                        UUID.randomUUID().toString(),
                        userId,
                        RESULT_BLOCKED,
                        srcIp,
                        normalizedCountry,
                        now.getHour(),
                        amount,
                        SAMPLE_TO_BANK,
                        SAMPLE_TO_ACCOUNT_ID
                );
                eventSender.send(event);

                return Map.of(
                        "status", RESULT_BLOCKED,
                        "message", "의심스러운 활동으로 인해 거래가 차단되었습니다.",
                        "amount", amount,
                        "toBank", SAMPLE_TO_BANK
                );
            }

            // MEDIUM: 추가 인증 필요 (blocked 상태는 변경하지 않음)
            if ("MEDIUM".equals(riskLevel)) {
                log.warn("TRANSFER_VERIFICATION_REQUIRED userId={} amount={} riskLevel=MEDIUM", userId, amount);

                return Map.of(
                        "status", RESULT_VERIFICATION_REQUIRED,
                        "message", "보안 확인이 필요합니다. 추가 인증을 완료해주세요.",
                        "amount", amount,
                        "toBank", SAMPLE_TO_BANK
                );
            }
        }

        // LOW 또는 VERIFIED: 정상 처리
        FdsEvent event = new FdsEvent(
                now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "TRANSFER",
                UUID.randomUUID().toString(),
                userId,
                RESULT_SUCCESS,
                srcIp,
                normalizedCountry,
                now.getHour(),
                amount,
                SAMPLE_TO_BANK,
                SAMPLE_TO_ACCOUNT_ID
        );

        eventSender.send(event);

        // ELK 로그
        log.info("TRANSFER_SUCCESS userId={} amount={} country={} srcIp={} timestamp={} toBank={} toAccountId={} riskLevel={}",
                userId, amount, normalizedCountry, srcIp, now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                SAMPLE_TO_BANK, SAMPLE_TO_ACCOUNT_ID, riskLevel);

        return Map.of(
                "status", RESULT_SUCCESS,
                "message", "송금이 성공적으로 처리되었습니다.",
                "amount", amount,
                "toBank", SAMPLE_TO_BANK
        );
    }

    /**
     * Google Sheets에서 Current_Total_Score를 조회하여 Risk Level 계산
     */
    private String getRiskLevel(String userId) {
        try {
            int score = googleSheetsService.getCurrentTotalScore(userId);

            if (score >= 70) {
                return "HIGH";
            } else if (score >= 40) {
                return "MEDIUM";
            } else {
                return "LOW";
            }
        } catch (Exception e) {
            log.error("Failed to get risk level for user: {}", userId, e);
            return "LOW"; // 기본값
        }
    }

    private String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            return "UNKNOWN";
        }
        return country.trim().toUpperCase(Locale.ROOT);
    }

    private String getClientIp(HttpServletRequest request, String country) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            ip = COUNTRY_IP_MAP.getOrDefault(country, "203.0.113.200");
        }

        return ip;
    }
}