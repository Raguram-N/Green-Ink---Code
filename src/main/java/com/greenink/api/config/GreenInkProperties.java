package com.greenink.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "greenink")
public record GreenInkProperties(
        Security security,
        Auth auth,
        Billing billing,
        Cors cors
) {
    public record Security(
            String jwtSecret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            boolean secureCookies
    ) {}

    public record Auth(
            String mode,
            String devOtp,
            boolean exposeDevOtp,
            Duration otpTtl,
            Duration resendAfter,
            int maxAttempts,
            List<String> adminIdentifiers
    ) {}

    public record Billing(String mode) {}

    public record Cors(List<String> allowedOrigins) {}
}
