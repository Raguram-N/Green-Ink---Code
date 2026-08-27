package com.greenink.api.auth;

import com.greenink.api.common.IntegrationNotConfiguredException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "greenink.auth.mode", havingValue = "not-configured", matchIfMissing = true)
public class NotConfiguredOtpProvider implements OtpProvider {
    @Override public OtpChallenge issue(String normalizedIdentifier) { throw new IntegrationNotConfiguredException("OTP provider"); }
    @Override public VerifiedIdentity verify(String challengeId, String otp) { throw new IntegrationNotConfiguredException("OTP provider"); }
}
