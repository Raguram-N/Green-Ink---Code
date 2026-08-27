package com.greenink.api.auth;

import com.greenink.api.auth.dto.AuthResponse;
import com.greenink.api.auth.dto.AuthUserResponse;
import com.greenink.api.auth.dto.OtpChallengeResponse;
import com.greenink.api.common.Hashing;
import com.greenink.api.common.UnauthorizedException;
import com.greenink.api.config.GreenInkProperties;
import com.greenink.api.security.JwtService;
import com.greenink.api.user.UserAccount;
import com.greenink.api.user.UserRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {
    private final OtpProvider otpProvider;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final JwtService jwtService;
    private final GreenInkProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            OtpProvider otpProvider,
            UserRepository userRepository,
            SessionRepository sessionRepository,
            JwtService jwtService,
            GreenInkProperties properties
    ) {
        this.otpProvider = otpProvider;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    public OtpChallengeResponse requestOtp(String rawIdentifier) {
        String identifier = IdentityNormalizer.normalize(rawIdentifier);
        OtpProvider.OtpChallenge challenge = otpProvider.issue(identifier);
        return new OtpChallengeResponse(challenge.challengeId(), challenge.expiresInSeconds(),
                challenge.resendAfterSeconds(), challenge.debugOtp());
    }

    public LoginResult verifyOtp(String challengeId, String otp) {
        OtpProvider.VerifiedIdentity verified = otpProvider.verify(challengeId, otp);
        String identifier = verified.normalizedIdentifier();
        Set<String> roles = rolesFor(identifier);
        UserAccount user = userRepository.findByIdentifier(identifier)
                .orElseGet(() -> userRepository.create(identifier, roles));
        if (!user.roles().equals(roles)) {
            user = userRepository.save(new UserAccount(user.id(), user.identifier(), roles, user.createdAt(), user.preferences()));
        }
        return issueSession(user);
    }

    public LoginResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException("REFRESH_TOKEN_REQUIRED", "Refresh token is required.");
        }
        String hash = Hashing.sha256(rawRefreshToken);
        AuthSession existing = sessionRepository.findByRefreshTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("REFRESH_TOKEN_INVALID", "Refresh token is invalid."));
        if (Instant.now().isAfter(existing.expiresAt())) {
            sessionRepository.deleteByRefreshTokenHash(hash);
            throw new UnauthorizedException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired.");
        }
        UserAccount user = userRepository.findById(existing.userId())
                .orElseThrow(() -> new UnauthorizedException("USER_NOT_FOUND", "User account no longer exists."));
        sessionRepository.deleteByRefreshTokenHash(hash);
        return issueSession(user);
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            sessionRepository.deleteByRefreshTokenHash(Hashing.sha256(rawRefreshToken));
        }
    }

    public void logoutAll(String userId) {
        sessionRepository.deleteAllByUserId(userId);
    }

    private LoginResult issueSession(UserAccount user) {
        String accessToken = jwtService.issueAccessToken(user);
        String refreshToken = newRefreshToken();
        Instant expiresAt = Instant.now().plus(properties.security().refreshTokenTtl());
        sessionRepository.save(new AuthSession(Hashing.sha256(refreshToken), user.id(), expiresAt, Instant.now()));
        AuthResponse response = new AuthResponse(
                accessToken,
                jwtService.accessTokenExpiresInSeconds(),
                new AuthUserResponse(user.id(), user.identifier(), user.roles()));
        return new LoginResult(response, refreshToken, properties.security().refreshTokenTtl().toSeconds());
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Set<String> rolesFor(String identifier) {
        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");
        if (properties.auth().adminIdentifiers() != null) {
            boolean admin = properties.auth().adminIdentifiers().stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(IdentityNormalizer::normalize)
                    .anyMatch(identifier::equals);
            if (admin) roles.add("ROLE_ADMIN");
        }
        return Set.copyOf(roles);
    }

    public record LoginResult(AuthResponse response, String refreshToken, long refreshMaxAgeSeconds) {}
}
