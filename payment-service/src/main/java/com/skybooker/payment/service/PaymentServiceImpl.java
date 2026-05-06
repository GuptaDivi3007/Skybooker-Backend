package com.skybooker.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.skybooker.payment.client.BookingClient;
import com.skybooker.payment.dto.*;
import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.entity.PaymentMethod;
import com.skybooker.payment.entity.PaymentStatus;
import com.skybooker.payment.exception.BadRequestException;
import com.skybooker.payment.exception.ResourceNotFoundException;
import com.skybooker.payment.repository.PaymentRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingClient bookingClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              BookingClient bookingClient,
                              @Value("${razorpay.key-id}") String razorpayKeyId,
                              @Value("${razorpay.key-secret}") String razorpayKeySecret) throws RazorpayException {
        this.paymentRepository = paymentRepository;
        this.bookingClient = bookingClient;
    }

    @Override
    public RazorpayOrderResponse createRazorpayOrder(String userId, PaymentRequest request) {
        String bookingId = normalizeRequired(request.bookingId(), "Booking id");
        String normalizedUserId = normalizeRequired(userId, "User id");

        if (request.amount() <= 0) {
            throw new BadRequestException("Amount must be greater than 0");
        }

        Payment payment = new Payment();
        payment.setUserId(normalizedUserId);
        payment.setBookingId(bookingId);
        payment.setAmount(request.amount());
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setReceipt("receipt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));

        try {
            int amountInPaise = toPaise(request.amount());

            System.out.println("Creating Razorpay Order...");
            System.out.println("Booking Id: " + bookingId);
            System.out.println("User Id: " + normalizedUserId);
            System.out.println("Amount: " + request.amount());
            System.out.println("Amount in paise: " + amountInPaise);
            System.out.println("Currency: " + currency);

            Map<String, Object> notes = new HashMap<>();
            notes.put("localPaymentId", payment.getReceipt());
            notes.put("bookingId", bookingId);
            notes.put("userId", normalizedUserId);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("amount", amountInPaise);
            requestBody.put("currency", currency);
            requestBody.put("receipt", payment.getReceipt());
            requestBody.put("notes", notes);

            String credentials = razorpayKeyId.trim() + ":" + razorpayKeySecret.trim();
            String basicAuth = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + basicAuth);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.razorpay.com/v1/orders",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new BadRequestException("Unable to create Razorpay order");
            }

            Map responseBody = response.getBody();

            String razorpayOrderId = String.valueOf(responseBody.get("id"));

            System.out.println("Razorpay Order Created Successfully");
            System.out.println("Razorpay Order Id: " + razorpayOrderId);

            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setGatewayResponse(responseBody.toString());

            Payment saved = paymentRepository.save(payment);

            return new RazorpayOrderResponse(
                    saved.getPaymentId(),
                    saved.getBookingId(),
                    saved.getUserId(),
                    saved.getAmount(),
                    toPaise(saved.getAmount()),
                    saved.getCurrency(),
                    saved.getStatus(),
                    razorpayKeyId,
                    saved.getRazorpayOrderId(),
                    saved.getReceipt(),
                    saved.getCreatedAt()
            );

        } catch (Exception ex) {
            System.out.println("Payment order creation failed: " + ex.getMessage());
            throw new BadRequestException("Unable to create payment order: " + ex.getMessage());
        }
    }

    @Override
    public PaymentResponse verifyRazorpayPayment(RazorpayPaymentVerifyRequest request) {
        Payment payment = findPayment(request.paymentId());

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Only PENDING payment can be verified");
        }

        if (!payment.getRazorpayOrderId().equals(request.razorpayOrderId())) {
            throw new BadRequestException("Razorpay order id does not match local payment record");
        }

        boolean validSignature = verifyPaymentSignature(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature()
        );

        if (!validSignature) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Invalid Razorpay payment signature");
            payment.setProcessedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            throw new BadRequestException("Invalid Razorpay payment signature");
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentMethod(parsePaymentMethodOrDefault(request.paymentMethod()));
        payment.setTransactionId(request.razorpayPaymentId());
        payment.setRazorpayPaymentId(request.razorpayPaymentId());
        payment.setRazorpaySignature(request.razorpaySignature());
        payment.setFailureReason(null);
        payment.setProcessedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        bookingClient.linkPayment(saved.getBookingId(), saved.getPaymentId());
        bookingClient.confirmBooking(saved.getBookingId());

        return mapToResponse(saved);
    }

    @Override
    public RazorpayWebhookResponse handleRazorpayWebhook(String payload, String razorpaySignature) {
        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
            throw new BadRequestException("Razorpay webhook secret is missing in application.yml");
        }

        boolean validSignature = verifyWebhookSignature(payload, razorpaySignature);
        if (!validSignature) {
            throw new BadRequestException("Invalid Razorpay webhook signature");
        }

        JSONObject root = new JSONObject(payload);
        String event = root.optString("event");

        if ("payment.captured".equals(event) || "order.paid".equals(event)) {
            JSONObject paymentJson = extractPaymentEntity(root);
            String razorpayPaymentId = paymentJson.optString("id");
            String razorpayOrderId = paymentJson.optString("order_id");
            String method = paymentJson.optString("method");

            Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found for Razorpay order: " + razorpayOrderId));

            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaymentMethod(parseRazorpayMethod(method));
                payment.setTransactionId(razorpayPaymentId);
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setGatewayResponse(payload);
                payment.setProcessedAt(LocalDateTime.now());
                Payment saved = paymentRepository.save(payment);

                bookingClient.linkPayment(saved.getBookingId(), saved.getPaymentId());
                bookingClient.confirmBooking(saved.getBookingId());
            }
        }

        if ("payment.failed".equals(event)) {
            JSONObject paymentJson = extractPaymentEntity(root);
            String razorpayOrderId = paymentJson.optString("order_id");

            paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(payment -> {
                if (payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason(paymentJson.optString("error_description", "Payment failed"));
                    payment.setGatewayResponse(payload);
                    payment.setProcessedAt(LocalDateTime.now());

                    Payment saved = paymentRepository.save(payment);

                    // Payment failed → cancel booking → release held seats
                    bookingClient.cancelBooking(saved.getBookingId());
                }
            });
        }

        return new RazorpayWebhookResponse("Webhook processed successfully");
    }

    @Override
    public PaymentResponse createPayment(String userId, PaymentRequest request) {
        Payment payment = new Payment();
        payment.setUserId(normalizeRequired(userId, "User id"));
        payment.setBookingId(normalizeRequired(request.bookingId(), "Booking id"));
        payment.setAmount(request.amount());
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.PENDING);

        Payment saved = paymentRepository.save(payment);
        return mapToResponse(saved);
    }

    @Override
    public PaymentResponse processPayment(String paymentId, ProcessPaymentRequest request) {
        Payment payment = findPayment(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Only PENDING payment can be processed");
        }

        PaymentMethod method = parsePaymentMethod(request.paymentMethod());

        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(generateTransactionId());
        payment.setFailureReason(null);
        payment.setProcessedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        bookingClient.linkPayment(saved.getBookingId(), saved.getPaymentId());
        bookingClient.confirmBooking(saved.getBookingId());

        return mapToResponse(saved);
    }

    @Override
    public PaymentResponse failPayment(String paymentId, PaymentFailureRequest request) {
        Payment payment = findPayment(paymentId);

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException("Only PENDING payment can be failed");
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(normalizeRequired(request.failureReason(), "Failure reason"));
        payment.setProcessedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        // Payment failed → cancel booking → release held seats
        bookingClient.cancelBooking(saved.getBookingId());

        return mapToResponse(saved);
    }

    @Override
    public PaymentResponse refundPayment(String paymentId) {
        Payment payment = findPayment(paymentId);

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BadRequestException("Only SUCCESS payment can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setProcessedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        bookingClient.cancelBooking(saved.getBookingId());

        return mapToResponse(saved);
    }

    @Override
    public PaymentResponse updateStatus(String paymentId, PaymentStatusUpdateRequest request) {
        Payment payment = findPayment(paymentId);

        if (request.status() == null) {
            throw new BadRequestException("Payment status is required");
        }

        payment.setStatus(request.status());

        if (request.status() == PaymentStatus.SUCCESS && payment.getTransactionId() == null) {
            payment.setTransactionId(generateTransactionId());
            payment.setProcessedAt(LocalDateTime.now());
        }

        if (request.status() == PaymentStatus.FAILED || request.status() == PaymentStatus.REFUNDED) {
            payment.setProcessedAt(LocalDateTime.now());
        }

        Payment saved = paymentRepository.save(payment);
        
        if (saved.getStatus() == PaymentStatus.FAILED) {
            bookingClient.cancelBooking(saved.getBookingId());
        }
        return mapToResponse(saved);
    }

    @Override
    public PaymentResponse getPaymentById(String paymentId) {
        return mapToResponse(findPayment(paymentId));
    }

    @Override
    public List<PaymentResponse> getMyPayments(String userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<PaymentResponse> getPaymentsByBooking(String bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PaymentResponse getLatestPaymentByBooking(String bookingId) {
        Payment payment = paymentRepository.findTopByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking id: " + bookingId));

        return mapToResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<PaymentSummaryResponse> getMyPaymentSummary(String userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(payment -> new PaymentSummaryResponse(
                        payment.getPaymentId(),
                        payment.getBookingId(),
                        payment.getAmount(),
                        payment.getStatus().name()
                ))
                .toList();
    }

    private Payment findPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
    }

    private int toPaise(double amount) {
        return (int) Math.round(amount * 100);
    }

    private PaymentMethod parsePaymentMethod(String value) {
        try {
            return PaymentMethod.valueOf(normalizeRequired(value, "Payment method"));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid payment method. Allowed: CARD, UPI, NET_BANKING, WALLET");
        }
    }

    private PaymentMethod parsePaymentMethodOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return PaymentMethod.UPI;
        }
        return parsePaymentMethod(value.trim().toUpperCase());
    }

    private PaymentMethod parseRazorpayMethod(String method) {
        if (method == null) return PaymentMethod.UPI;
        return switch (method.toLowerCase()) {
            case "card" -> PaymentMethod.CARD;
            case "netbanking" -> PaymentMethod.NET_BANKING;
            case "wallet" -> PaymentMethod.WALLET;
            default -> PaymentMethod.UPI;
        };
    }

    private String generateTransactionId() {
        String transactionId;
        do {
            transactionId = "TXN-" + UUID.randomUUID();
        } while (paymentRepository.existsByTransactionId(transactionId));
        return transactionId;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        return value.trim();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getBookingId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getPaymentMethod() == null ? null : payment.getPaymentMethod().name(),
                payment.getRazorpayOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        String payload = orderId + "|" + paymentId;
        String expectedSignature = hmacSha256(payload, razorpayKeySecret);
        return secureEquals(expectedSignature, signature);
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        String expectedSignature = hmacSha256(payload, razorpayWebhookSecret);
        return secureEquals(expectedSignature, signature);
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new BadRequestException("Unable to verify Razorpay signature");
        }
    }

    private boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private JSONObject extractPaymentEntity(JSONObject root) {
        try {
            return root.getJSONObject("payload")
                    .getJSONObject("payment")
                    .getJSONObject("entity");
        } catch (Exception ex) {
            throw new BadRequestException("Invalid Razorpay webhook payload");
        }
    }
}
