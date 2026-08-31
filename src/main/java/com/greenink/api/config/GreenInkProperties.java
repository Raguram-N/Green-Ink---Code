package com.greenink.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "greenink")
public record GreenInkProperties(
        Api api,
        Security security,
        Auth auth,
        Billing billing,
        Content content,
        Cors cors
) {
    public record Api(String basePath) {}

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

    public record Content(String mode, String localPath) {}

    public record Cors(List<String> allowedOrigins) {}
}

