# Razorpay production adapter TODO

`DemoPaymentGateway` exists only to exercise the service flow during review.

Create a production `PaymentGateway` implementation, preferably under:

```text
com.greenink.api.infrastructure.razorpay.RazorpayPaymentGateway
```

It must use server-side credentials and verify both checkout signatures and webhook signatures. Do not trust `amount`, `plan`, `premium`, or payment status sent by the browser.

Before production, add durable idempotency around provider order IDs/payment IDs/webhook event IDs and define refund/cancellation/expiry behavior.
