package com.greenink.api.auth;

import com.greenink.api.auth.dto.AuthResponse;
import com.greenink.api.auth.dto.OtpChallengeResponse;
import com.greenink.api.auth.dto.OtpRequest;
import com.greenink.api.auth.dto.OtpVerifyRequest;
import com.greenink.api.config.GreenInkProperties;
import com.greenink.api.security.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public static final String REFRESH_COOKIE = "gi_refresh";

    private final AuthService authService;
    private final GreenInkProperties properties;

    public AuthController(AuthService authService, GreenInkProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/otp/request")
    public OtpChallengeResponse requestOtp(@Valid @RequestBody OtpRequest request) {
        return authService.requestOtp(request.identifier());
    }

    @PostMapping("/otp/resend")
    public OtpChallengeResponse resendOtp(@Valid @RequestBody OtpRequest request) {
        return authService.requestOtp(request.identifier());
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        AuthService.LoginResult result = authService.verifyOtp(request.challengeId(), request.otp());
        return withRefreshCookie(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        AuthService.LoginResult result = authService.refresh(refreshToken);
        return withRefreshCookie(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString()).build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        authService.logoutAll(SecurityUtil.requireUserId());
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString()).build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(AuthService.LoginResult result) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, result.refreshToken())
                .httpOnly(true)
                .secure(properties.security().secureCookies())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(result.refreshMaxAgeSeconds())
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result.response());
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(properties.security().secureCookies())
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
    }
}
