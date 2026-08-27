package com.greenink.api.billing;

import com.greenink.api.billing.dto.BillingOrderRequest;
import com.greenink.api.billing.dto.BillingOrderResponse;
import com.greenink.api.billing.dto.PaymentVerifyRequest;
import com.greenink.api.billing.dto.PaymentVerifyResponse;
import com.greenink.api.billing.dto.PlanResponse;
import com.greenink.api.billing.dto.SubscriptionResponse;
import com.greenink.api.security.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class BillingController {
    private final PlanService planService;
    private final BillingService billingService;

    public BillingController(PlanService planService, BillingService billingService) {
        this.planService = planService;
        this.billingService = billingService;
    }

    @GetMapping("/plans")
    public List<PlanResponse> plans() { return planService.publicPlans(); }

    @PostMapping("/billing/orders")
    public BillingOrderResponse createOrder(@Valid @RequestBody BillingOrderRequest request) {
        return billingService.createOrder(SecurityUtil.requireUserId(), request.planCode());
    }

    @PostMapping("/billing/payments/verify")
    public PaymentVerifyResponse verify(@Valid @RequestBody PaymentVerifyRequest request) {
        return billingService.verify(SecurityUtil.requireUserId(), request.orderId(), request.paymentId(), request.signature());
    }

    @GetMapping("/billing/subscription")
    public SubscriptionResponse subscription() { return billingService.subscription(SecurityUtil.requireUserId()); }

    @GetMapping("/billing/payments")
    public List<PaymentRecord> payments() { return billingService.payments(SecurityUtil.requireUserId()); }

    @PostMapping("/webhooks/razorpay")
    public ResponseEntity<Void> webhook(
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature,
            @RequestBody String rawPayload) {
        billingService.handleWebhook(rawPayload, signature);
        return ResponseEntity.noContent().build();
    }
}
