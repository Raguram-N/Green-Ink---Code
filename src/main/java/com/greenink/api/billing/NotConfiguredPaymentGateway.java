package com.greenink.api.billing;

import com.greenink.api.common.IntegrationNotConfiguredException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "greenink.billing.mode", havingValue = "not-configured", matchIfMissing = true)
public class NotConfiguredPaymentGateway implements PaymentGateway {
    @Override public GatewayOrder createOrder(String userId, PlanDefinition plan) { throw new IntegrationNotConfiguredException("Razorpay"); }
    @Override public boolean verifyPayment(String orderId, String paymentId, String signature) { throw new IntegrationNotConfiguredException("Razorpay"); }
    @Override public GatewayWebhook verifyWebhook(String rawPayload, String signature) { throw new IntegrationNotConfiguredException("Razorpay"); }
}
