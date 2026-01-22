package com.fds.controller;

import com.fds.dto.LoginRequest;
import com.fds.dto.User;
import com.fds.service.AuthService;
import com.fds.service.GoogleSheetsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleSheetsService googleSheetsService;

    @PostMapping("/login")
    public String login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String result = authService.login(
                    request.userId(),
                    request.password(),
                    request.country(),
                    httpRequest
                );

        if (result.equals("BLOCKED")) {
            return "LOGIN_BLOCKED - 의심스러운 활동으로 인해 계정이 차단되었습니다.";
        }

        return result.equals("SUCCESS") ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
    }

    @PostMapping("/logout")
    public String logout(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String result = authService.logout(
                        request.userId(),
                        request.country(),
                        httpRequest
                    );

        return result.equals("SUCCESS") ? "LOGOUT_SUCCESS" : "LOGOUT_FAILURE";
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verify(@RequestBody LoginRequest request) {
        User user = AuthService.getUser(request.userId());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("USER_NOT_FOUND");
        }

        if (!user.getPassword().equals(request.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("INVALID_PASSWORD");
        }

        // 이벤트 전송 없이 비밀번호만 검증
        return ResponseEntity.ok("VERIFIED");
    }

    @GetMapping("/check-blocked")
    public ResponseEntity<Map<String, Boolean>> checkBlocked(@RequestParam String userId) {
        boolean blocked = googleSheetsService.isUserBlocked(userId);
        return ResponseEntity.ok(Map.of("blocked", blocked));
    }

}

