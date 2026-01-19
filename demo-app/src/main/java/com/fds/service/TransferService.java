package com.fds.service;

import com.fds.dto.FdsEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final EventSender eventSender;

    public void transfer(String userId, Long amount, String requestIp, String userAgent) {



        FdsEvent event = new FdsEvent(
                "TRANSFER",
                userId,
                amount,
                null,
                requestIp,
                userAgent,
                LocalDateTime.now()
        );

        eventSender.send(event);
    }
}
