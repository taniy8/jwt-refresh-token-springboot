package com.example.auth.controller;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.entity.User;
import com.example.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request,
                                                        HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        setRefreshTokenCookie(response, auth.getRefreshToken());
        return ResponseEntity.ok(Map.of("accessToken", auth.getAccessToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request,
                                                     HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        setRefreshTokenCookie(response, auth.getRefreshToken());
        return ResponseEntity.ok(Map.of("accessToken", auth.getAccessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(
            @CookieValue(name = "refresh_token", required = false) String token,
            HttpServletResponse response) {

        if (token == null) {
            return ResponseEntity.status(401).body(Map.of("error", "No refresh token found"));
        }

        AuthResponse auth = authService.refresh(token);
        setRefreshTokenCookie(response, auth.getRefreshToken());
        return ResponseEntity.ok(Map.of("accessToken", auth.getAccessToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = "refresh_token", required = false) String token,
            HttpServletResponse response) {

        if (token != null) {
            authService.logout(token);
        }

        clearRefreshTokenCookie(response);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "name", user.getName()
        ));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(REFRESH_TOKEN_COOKIE_MAX_AGE);
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}