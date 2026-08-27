package com.greenink.api.billing;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    PaymentRecord save(PaymentRecord payment);
    Optional<PaymentRecord> findByOrderId(String orderId);
    List<PaymentRecord> findByUserId(String userId);
}
