package com.greenink.api.auth;

import com.greenink.api.common.BadRequestException;
import com.greenink.api.common.RateLimitException;
import com.greenink.api.config.GreenInkProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "greenink.auth.mode", havingValue = "dev")
public class DevOtpProvider implements OtpProvider {
    private final GreenInkProperties properties;
    private final ConcurrentHashMap<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastIssuedByIdentifier = new ConcurrentHashMap<>();

    public DevOtpProvider(GreenInkProperties properties) {
        this.properties = properties;
    }

    @Override
    public OtpChallenge issue(String normalizedIdentifier) {
        Instant now = Instant.now();
        Instant last = lastIssuedByIdentifier.get(normalizedIdentifier);
        if (last != null && now.isBefore(last.plus(properties.auth().resendAfter()))) {
            long wait = Math.max(1, last.plus(properties.auth().resendAfter()).getEpochSecond() - now.getEpochSecond());
            throw new RateLimitException("OTP_RESEND_TOO_SOON", "Please wait " + wait + " seconds before requesting another OTP.");
        }
        String id = "otp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Challenge challenge = new Challenge(normalizedIdentifier, properties.auth().devOtp(), now.plus(properties.auth().otpTtl()), 0);
        challenges.put(id, challenge);
        lastIssuedByIdentifier.put(normalizedIdentifier, now);
        String debugOtp = properties.auth().exposeDevOtp() ? properties.auth().devOtp() : null;
        return new OtpChallenge(id, properties.auth().otpTtl().toSeconds(), properties.auth().resendAfter().toSeconds(), debugOtp);
    }

    @Override
    public VerifiedIdentity verify(String challengeId, String otp) {
        Challenge current = challenges.get(challengeId);
        if (current == null) throw new BadRequestException("OTP_CHALLENGE_NOT_FOUND", "OTP challenge is invalid or already used.");
        if (Instant.now().isAfter(current.expiresAt())) {
            challenges.remove(challengeId);
            throw new BadRequestException("OTP_EXPIRED", "OTP has expired. Request a new OTP.");
        }
        if (current.attempts() >= properties.auth().maxAttempts()) {
            challenges.remove(challengeId);
            throw new BadRequestException("OTP_ATTEMPTS_EXCEEDED", "Too many incorrect OTP attempts. Request a new OTP.");
        }
        if (!current.otp().equals(otp)) {
            challenges.put(challengeId, new Challenge(current.identifier(), current.otp(), current.expiresAt(), current.attempts() + 1));
            throw new BadRequestException("OTP_INVALID", "Incorrect OTP.");
        }
        challenges.remove(challengeId);
        return new VerifiedIdentity(current.identifier());
    }

    private record Challenge(String identifier, String otp, Instant expiresAt, int attempts) {}
}
