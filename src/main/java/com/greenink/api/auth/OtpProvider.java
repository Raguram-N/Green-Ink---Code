package com.greenink.api.auth;

public interface OtpProvider {
    OtpChallenge issue(String normalizedIdentifier);
    VerifiedIdentity verify(String challengeId, String otp);

    record OtpChallenge(String challengeId, long expiresInSeconds, long resendAfterSeconds, String debugOtp) {}
    record VerifiedIdentity(String normalizedIdentifier) {}
}
