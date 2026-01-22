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
    private static final String RESULT_FORCE_LOGOUT = "FORCE_LOGOUT";
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

    public Map<String, Object> processTransfer(String userId, Long amount, String country, Boolean verified, HttpServletRequest request) {
        ZonedDateTime now = ZonedDateTime.now();
        String normalizedCountry = normalizeCountry(country);
        String srcIp = getClientIp(request, normalizedCountry);

        // Redis 카운트 증가
        incrementTransferCount(userId);

        // 1. Blocked 상태 체크
        if (googleSheetsService.isUserBlocked(userId)) {
            log.warn("TRANSFER_BLOCKED userId={} amount={} reason=BLOCKED_IN_SHEETS", userId, amount);
            return createForceLogoutResponse(amount, "계정이 차단되었습니다.");
        }

        // 2. Risk Level 체크 및 처리
        String riskLevel = evaluateRiskLevel(userId, verified, amount);

        if (RESULT_FORCE_LOGOUT.equals(riskLevel)) {
            return createForceLogoutResponse(amount, "의심스러운 활동이 감지되어 자동 로그아웃됩니다.");
        }

        if (RESULT_VERIFICATION_REQUIRED.equals(riskLevel)) {
            return createVerificationRequiredResponse(amount);
        }

        // 3. 정상 처리
        sendTransferEvent(userId, amount, normalizedCountry, srcIp, now);

        log.info("TRANSFER_SUCCESS userId={} amount={} country={} srcIp={} timestamp={} toBank={} riskLevel={}",
                userId, amount, normalizedCountry, srcIp, now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                SAMPLE_TO_BANK, riskLevel);

        return Map.of(
                "status", RESULT_SUCCESS,
                "message", "송금이 성공적으로 처리되었습니다.",
                "amount", amount,
                "toBank", SAMPLE_TO_BANK
        );
    }

    private void incrementTransferCount(String userId) {
        String redisKey = "tx_count:" + userId;
        Long txCount = redisTemplate.opsForValue().increment(redisKey);

        if (txCount != null && txCount == 1) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(10));
            log.info("Redis key created: {} with TTL 10 minutes", redisKey);
        }
        log.info("User {} transfer count in 10min: {}", userId, txCount);
    }

    private String evaluateRiskLevel(String userId, Boolean verified, Long amount) {
        int score = googleSheetsService.getCurrentTotalScore(userId);
        String riskLevel = calculateRiskLevel(score);

        if (Boolean.TRUE.equals(verified)) {
            log.info("VERIFIED_TRANSFER userId={} amount={} score={} riskLevel={}", userId, amount, score, riskLevel);

            if ("HIGH".equals(riskLevel)) {
                log.warn("TRANSFER_AUTO_BLOCKED userId={} amount={} riskLevel=HIGH (auto-blocking on verified transfer)", userId, amount);
                googleSheetsService.blockUser(userId);
                return RESULT_FORCE_LOGOUT;
            }

            return "VERIFIED";
        }

        // verified=false인 경우
        if ("HIGH".equals(riskLevel)) {
            log.warn("TRANSFER_AUTO_BLOCKED userId={} amount={} riskLevel=HIGH (auto-blocking)", userId, amount);
            googleSheetsService.blockUser(userId);
            return RESULT_FORCE_LOGOUT;
        }

        if ("MEDIUM".equals(riskLevel)) {
            log.warn("TRANSFER_VERIFICATION_REQUIRED userId={} amount={} riskLevel=MEDIUM", userId, amount);
            return RESULT_VERIFICATION_REQUIRED;
        }

        return riskLevel;
    }

    private String calculateRiskLevel(int score) {
        if (score >= 70) {
            return "HIGH";
        } else if (score >= 40) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private void sendTransferEvent(String userId, Long amount, String country, String srcIp, ZonedDateTime now) {
        FdsEvent event = new FdsEvent(
                now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                "TRANSFER",
                UUID.randomUUID().toString(),
                userId,
                RESULT_SUCCESS,
                srcIp,
                country,
                now.getHour(),
                amount,
                SAMPLE_TO_BANK,
                SAMPLE_TO_ACCOUNT_ID
        );
        eventSender.send(event);
    }

    private Map<String, Object> createForceLogoutResponse(Long amount, String message) {
        return Map.of(
                "status", RESULT_FORCE_LOGOUT,
                "message", message,
                "amount", amount,
                "toBank", SAMPLE_TO_BANK
        );
    }

    private Map<String, Object> createVerificationRequiredResponse(Long amount) {
        return Map.of(
                "status", RESULT_VERIFICATION_REQUIRED,
                "message", "보안 확인이 필요합니다. 추가 인증을 완료해주세요.",
                "amount", amount,
                "toBank", SAMPLE_TO_BANK
        );
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