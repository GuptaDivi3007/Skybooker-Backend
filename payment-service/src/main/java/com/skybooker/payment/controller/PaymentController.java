package com.skybooker.payment.controller;

import com.skybooker.payment.dto.*;
import com.skybooker.payment.entity.PaymentStatus;
import com.skybooker.payment.exception.AccessDeniedException;
import com.skybooker.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/razorpay/order")
    public RazorpayOrderResponse createRazorpayOrder(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String userId,
            @Valid @RequestBody PaymentRequest request) {
        requireAuthenticated(userId);
        return paymentService.createRazorpayOrder(userId, request);
    }

    @PostMapping("/razorpay/verify")
    public PaymentResponse verifyRazorpayPayment(@Valid @RequestBody RazorpayPaymentVerifyRequest request) {
        return paymentService.verifyRazorpayPayment(request);
    }

    @PostMapping(value = "/razorpay/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public RazorpayWebhookResponse razorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String razorpaySignature) {
        return paymentService.handleRazorpayWebhook(payload, razorpaySignature);
    }

    @PostMapping
    public PaymentResponse createPayment(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String userId,
            @Valid @RequestBody PaymentRequest request) {
        requireAuthenticated(userId);
        return paymentService.createPayment(userId, request);
    }

    @PutMapping("/{paymentId}/process")
    public PaymentResponse processPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody ProcessPaymentRequest request) {
        return paymentService.processPayment(paymentId, request);
    }

    @PutMapping("/{paymentId}/fail")
    public PaymentResponse failPayment(
            @PathVariable String paymentId,
            @Valid @RequestBody PaymentFailureRequest request) {
        return paymentService.failPayment(paymentId, request);
    }

    @PutMapping("/{paymentId}/refund")
    public PaymentResponse refundPayment(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String paymentId) {
        requireAdminOrInternal(role);
        return paymentService.refundPayment(paymentId);
    }

    @PutMapping("/{paymentId}/status")
    public PaymentResponse updateStatus(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String paymentId,
            @Valid @RequestBody PaymentStatusUpdateRequest request) {
        requireAdminOrInternal(role);
        return paymentService.updateStatus(paymentId, request);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPaymentById(@PathVariable String paymentId) {
        return paymentService.getPaymentById(paymentId);
    }

    @GetMapping
    public List<PaymentResponse> getMyPayments(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String userId) {
        requireAuthenticated(userId);
        return paymentService.getMyPayments(userId);
    }

    @GetMapping("/summary")
    public List<PaymentSummaryResponse> getMyPaymentSummary(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String userId) {
        requireAuthenticated(userId);
        return paymentService.getMyPaymentSummary(userId);
    }

    @GetMapping("/booking/{bookingId}")
    public List<PaymentResponse> getPaymentsByBooking(@PathVariable String bookingId) {
        return paymentService.getPaymentsByBooking(bookingId);
    }

    @GetMapping("/booking/{bookingId}/latest")
    public PaymentResponse getLatestPaymentByBooking(@PathVariable String bookingId) {
        return paymentService.getLatestPaymentByBooking(bookingId);
    }

    @GetMapping("/status/{status}")
    public List<PaymentResponse> getPaymentsByStatus(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable PaymentStatus status) {
        requireAdminOrInternal(role);
        return paymentService.getPaymentsByStatus(status);
    }

    private void requireAuthenticated(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("Authenticated user is required");
        }
    }

    private void requireAdminOrInternal(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("INTERNAL"))) {
            throw new AccessDeniedException("Only ADMIN or INTERNAL can perform this action");
        }
    }
}
