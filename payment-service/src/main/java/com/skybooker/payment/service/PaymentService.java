package com.skybooker.payment.service;

import com.skybooker.payment.dto.*;
import com.skybooker.payment.entity.PaymentStatus;

import java.util.List;

public interface PaymentService {

    RazorpayOrderResponse createRazorpayOrder(String userId, PaymentRequest request);

    PaymentResponse verifyRazorpayPayment(RazorpayPaymentVerifyRequest request);

    RazorpayWebhookResponse handleRazorpayWebhook(String payload, String razorpaySignature);

    PaymentResponse createPayment(String userId, PaymentRequest request);

    PaymentResponse processPayment(String paymentId, ProcessPaymentRequest request);

    PaymentResponse failPayment(String paymentId, PaymentFailureRequest request);

    PaymentResponse refundPayment(String paymentId);

    PaymentResponse updateStatus(String paymentId, PaymentStatusUpdateRequest request);

    PaymentResponse getPaymentById(String paymentId);

    List<PaymentResponse> getMyPayments(String userId);

    List<PaymentResponse> getPaymentsByBooking(String bookingId);

    PaymentResponse getLatestPaymentByBooking(String bookingId);

    List<PaymentResponse> getPaymentsByStatus(PaymentStatus status);

    List<PaymentSummaryResponse> getMyPaymentSummary(String userId);
}
