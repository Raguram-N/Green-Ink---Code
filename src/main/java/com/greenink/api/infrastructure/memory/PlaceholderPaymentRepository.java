package com.greenink.api.infrastructure.memory;

import com.greenink.api.billing.PaymentRecord;
import com.greenink.api.billing.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PlaceholderPaymentRepository implements PaymentRepository {
    private final ConcurrentHashMap<String, PaymentRecord> byOrder = new ConcurrentHashMap<>();
    @Override public PaymentRecord save(PaymentRecord payment) { byOrder.put(payment.orderId(), payment); return payment; }
    @Override public Optional<PaymentRecord> findByOrderId(String orderId) { return Optional.ofNullable(byOrder.get(orderId)); }
    @Override public List<PaymentRecord> findByUserId(String userId) {
        return byOrder.values().stream().filter(p -> p.userId().equals(userId))
                .sorted(Comparator.comparing(PaymentRecord::createdAt).reversed()).toList();
    }
}
