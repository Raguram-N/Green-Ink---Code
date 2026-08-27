package com.greenink.api.auth.dto;

public record OtpChallengeResponse(
        String challengeId,
        long expiresIn,
        long resendAfter,
        String debugOtp
) {}
