// service/AuthService.java
package com.fds.service;

import com.fds.dto.FdsEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EventSender eventSender;

    public void login(String userId, String ip, String userAgent) {


        boolean success = true;

        if (success) {
            FdsEvent event = new FdsEvent(
                    "LOGIN",
                    userId,
                    null,
                    ip,
                    ip,
                    userAgent,
                    LocalDateTime.now()
            );

            eventSender.send(event);
        }
    }
}
