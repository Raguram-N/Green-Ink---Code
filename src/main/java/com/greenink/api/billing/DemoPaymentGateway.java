package com.greenink.api.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenink.api.common.BadRequestException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "greenink.billing.mode", havingValue = "demo")
public class DemoPaymentGateway implements PaymentGateway {
    private final ObjectMapper objectMapper;

    public DemoPaymentGateway(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Override
    public GatewayOrder createOrder(String userId, PlanDefinition plan) {
        String orderId = "demo_order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return new GatewayOrder(orderId, plan.amountPaise(), plan.currency(), "rzp_test_REPLACE_IN_REAL_ADAPTER");
    }

    @Override
    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        return paymentId != null && !paymentId.isBlank() && "dev-valid-signature".equals(signature);
    }

    @Override
    public GatewayWebhook verifyWebhook(String rawPayload, String signature) {
        if (!"dev-webhook-signature".equals(signature)) {
            throw new BadRequestException("WEBHOOK_SIGNATURE_INVALID", "Webhook signature is invalid.");
        }
        try {
            JsonNode node = objectMapper.readTree(rawPayload);
            return new GatewayWebhook(node.path("orderId").asText(), node.path("paymentId").asText(), node.path("status").asText());
        } catch (Exception e) {
            throw new BadRequestException("WEBHOOK_PAYLOAD_INVALID", "Webhook payload is invalid.");
        }
    }
}
