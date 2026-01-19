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
            @RequestBody LoginRequest request
    ) {
        authService.login(
                request.userId(),
                request.country()
        );

        return "LOGIN_SUCCESS";
    }

    @PostMapping("/logout")
    public String logout(
            @RequestBody LoginRequest request
    ) {
        authService.logout(
                request.userId(),
                request.country()
        );

        return "LOGOUT_SUCCESS";
    }
}

