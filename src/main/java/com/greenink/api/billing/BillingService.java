package com.greenink.api.billing;

import com.greenink.api.billing.dto.BillingOrderResponse;
import com.greenink.api.billing.dto.PaymentVerifyResponse;
import com.greenink.api.billing.dto.SubscriptionResponse;
import com.greenink.api.common.BadRequestException;
import com.greenink.api.common.NotFoundException;
import com.greenink.api.entitlement.Subscription;
import com.greenink.api.entitlement.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class BillingService {
    private final PlanService planService;
    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;

    public BillingService(PlanService planService, PaymentGateway paymentGateway, PaymentRepository paymentRepository,
                          SubscriptionRepository subscriptionRepository) {
        this.planService = planService;
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public BillingOrderResponse createOrder(String userId, String planCode) {
        PlanDefinition plan = planService.require(planCode);
        PaymentGateway.GatewayOrder gateway = paymentGateway.createOrder(userId, plan);
        paymentRepository.save(new PaymentRecord(gateway.orderId(), userId, plan.code(), plan.amountPaise(), plan.currency(),
                "PENDING", null, Instant.now(), null));
        return new BillingOrderResponse(gateway.orderId(), plan.code(), gateway.amountPaise(), gateway.currency(), gateway.publicKey());
    }

    public PaymentVerifyResponse verify(String userId, String orderId, String paymentId, String signature) {
        PaymentRecord payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("PAYMENT_ORDER_NOT_FOUND", "Payment order not found."));
        if (!payment.userId().equals(userId)) throw new BadRequestException("PAYMENT_ORDER_OWNER_MISMATCH", "Payment order belongs to another account.");
        if ("PAID".equals(payment.status())) return toVerifyResponse(subscriptionRepository.findByUserId(userId).orElseThrow());
        if (!paymentGateway.verifyPayment(orderId, paymentId, signature)) {
            throw new BadRequestException("PAYMENT_SIGNATURE_INVALID", "Payment signature verification failed.");
        }
        markPaid(payment, paymentId);
        Subscription subscription = activate(userId, payment.planCode());
        return toVerifyResponse(subscription);
    }

    public void handleWebhook(String rawPayload, String signature) {
        PaymentGateway.GatewayWebhook webhook = paymentGateway.verifyWebhook(rawPayload, signature);
        if (!"captured".equalsIgnoreCase(webhook.status()) && !"paid".equalsIgnoreCase(webhook.status())) return;
        PaymentRecord payment = paymentRepository.findByOrderId(webhook.orderId())
                .orElseThrow(() -> new NotFoundException("PAYMENT_ORDER_NOT_FOUND", "Payment order not found."));
        if (!"PAID".equals(payment.status())) {
            markPaid(payment, webhook.paymentId());
            activate(payment.userId(), payment.planCode());
        }
    }

    public SubscriptionResponse subscription(String userId) {
        Subscription s = subscriptionRepository.findByUserId(userId).orElse(null);
        if (s == null) return new SubscriptionResponse("NONE", null, false, null, null);
        boolean active = s.activeAt(Instant.now());
        return new SubscriptionResponse(active ? "ACTIVE" : "EXPIRED", s.planCode(), active, s.startedAt(), s.expiresAt());
    }

    public List<PaymentRecord> payments(String userId) { return paymentRepository.findByUserId(userId); }

    private void markPaid(PaymentRecord payment, String paymentId) {
        paymentRepository.save(new PaymentRecord(payment.orderId(), payment.userId(), payment.planCode(), payment.amountPaise(),
                payment.currency(), "PAID", paymentId, payment.createdAt(), Instant.now()));
    }

    private Subscription activate(String userId, String planCode) {
        PlanDefinition plan = planService.require(planCode);
        Instant now = Instant.now();
        Subscription existing = subscriptionRepository.findByUserId(userId).orElse(null);
        Instant base = existing != null && existing.activeAt(now) ? existing.expiresAt() : now;
        Instant expiresAt = ZonedDateTime.ofInstant(base, ZoneOffset.UTC).plusMonths(plan.months()).toInstant();
        Instant startedAt = existing != null && existing.activeAt(now) ? existing.startedAt() : now;
        Subscription updated = new Subscription(userId, plan.code(), "ACTIVE", startedAt, expiresAt, now);
        return subscriptionRepository.save(updated);
    }

    private PaymentVerifyResponse toVerifyResponse(Subscription subscription) {
        boolean active = subscription.activeAt(Instant.now());
        return new PaymentVerifyResponse("VERIFIED", subscription.planCode(), active, subscription.expiresAt());
    }
}
