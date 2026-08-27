package com.greenink.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpRequest(@NotBlank String identifier) {}
