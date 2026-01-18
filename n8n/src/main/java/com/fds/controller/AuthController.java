// controller/AuthController.java
package com.fds.controller;

import com.fds.dto.LoginRequest;
import com.fds.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public String login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.login(
                request.userId(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );

        return "LOGIN_SUCCESS";
    }
}
