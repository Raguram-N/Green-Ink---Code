package com.greenink.api.common;

import org.springframework.http.HttpStatus;

public class IntegrationNotConfiguredException extends ApiException {
    public IntegrationNotConfiguredException(String integration) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "INTEGRATION_NOT_CONFIGURED",
                integration + " is not configured for this environment.");
    }
}
