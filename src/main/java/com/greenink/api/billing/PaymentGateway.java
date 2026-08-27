package com.greenink.api.billing;

public interface PaymentGateway {
    GatewayOrder createOrder(String userId, PlanDefinition plan);
    boolean verifyPayment(String orderId, String paymentId, String signature);
    GatewayWebhook verifyWebhook(String rawPayload, String signature);

    record GatewayOrder(String orderId, long amountPaise, String currency, String publicKey) {}
    record GatewayWebhook(String orderId, String paymentId, String status) {}
}
