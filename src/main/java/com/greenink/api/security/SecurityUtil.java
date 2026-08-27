package com.greenink.api.security;

import com.greenink.api.common.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static Optional<String> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.ofNullable(authentication.getName());
    }

    public static String requireUserId() {
        return currentUserId().orElseThrow(() -> new UnauthorizedException("AUTHENTICATION_REQUIRED", "Authentication is required."));
    }
}
