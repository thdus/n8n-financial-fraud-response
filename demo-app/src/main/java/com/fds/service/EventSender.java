package com.fds.service;

import com.fds.dto.FdsEvent;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventSender {

    private final WebClient webClient;
    private final HttpServletResponse response;

    public void send(FdsEvent event) {
        webClient.post()
                .uri("/webhook/fds-test") // n8n Webhook URL
                .bodyValue(event)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(esponse ->
                        log.info("n8n response = {}", response)); // 비동기
    }
}
