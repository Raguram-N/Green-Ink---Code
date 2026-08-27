package com.greenink.api.auth;

import com.greenink.api.common.BadRequestException;

import java.util.Locale;

public final class IdentityNormalizer {
    private IdentityNormalizer() {}

    public static String normalize(String identifier) {
        if (identifier == null) throw new BadRequestException("INVALID_IDENTIFIER", "Mobile number or email ID is required.");
        String value = identifier.trim();
        if (value.contains("@")) {
            String email = value.toLowerCase(Locale.ROOT);
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                throw new BadRequestException("INVALID_IDENTIFIER", "Enter a valid email ID.");
            }
            return email;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        if (digits.length() != 10) {
            throw new BadRequestException("INVALID_IDENTIFIER", "Enter a valid 10-digit mobile number or email ID.");
        }
        return digits;
    }
}
