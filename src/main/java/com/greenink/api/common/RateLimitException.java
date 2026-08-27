package com.greenink.api.common;

import org.springframework.http.HttpStatus;

public class RateLimitException extends ApiException {
    public RateLimitException(String code, String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, code, message);
    }
}
