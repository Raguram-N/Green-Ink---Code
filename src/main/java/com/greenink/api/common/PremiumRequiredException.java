package com.greenink.api.common;

import org.springframework.http.HttpStatus;

public class PremiumRequiredException extends ApiException {
    public PremiumRequiredException(String message) {
        super(HttpStatus.FORBIDDEN, "PREMIUM_REQUIRED", message);
    }
}
