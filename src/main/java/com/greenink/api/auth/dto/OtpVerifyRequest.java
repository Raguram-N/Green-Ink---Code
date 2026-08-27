package com.greenink.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank String challengeId,
        @NotBlank @Pattern(regexp = "\\d{4}", message = "OTP must contain exactly 4 digits") String otp
) {}
