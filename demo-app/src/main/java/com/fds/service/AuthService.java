package com.fds.service;

import com.fds.dto.FdsEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final Map<String, String> COUNTRY_IP_MAP = Map.of(
            "KR", "203.0.113.10",
            "US", "198.51.100.23",
            "JP", "192.0.2.44",
            "SG", "203.0.113.77",
            "GB", "198.51.100.88"
    );

    private final EventSender eventSender;

    public void login(String userId, String country) {
        FdsEvent event = createAuthEvent("LOGIN", userId, country);
        eventSender.send(event);
    }

    public void logout(String userId, String country) {
        FdsEvent event = createAuthEvent("LOGOUT", userId, country);
        eventSender.send(event);
    }

    private FdsEvent createAuthEvent(String eventType, String userId, String country) {
        ZonedDateTime now = ZonedDateTime.now();
        String normalizedCountry = normalizeCountry(country);
        return new FdsEvent(
                now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                eventType,
                UUID.randomUUID().toString(),
                userId,
                RESULT_SUCCESS,
                resolveSrcIp(normalizedCountry),
                normalizedCountry,
                now.getHour(),
                null,
                null,
                null
        );
    }

    private String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            return "UNKNOWN";
        }
        return country.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveSrcIp(String country) {
        return COUNTRY_IP_MAP.getOrDefault(country, "203.0.113.200");
    }
}

