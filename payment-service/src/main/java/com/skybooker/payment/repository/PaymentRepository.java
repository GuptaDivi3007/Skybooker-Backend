package com.skybooker.payment.repository;

import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Payment> findByBookingId(String bookingId);

    Optional<Payment> findTopByBookingIdOrderByCreatedAtDesc(String bookingId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    List<Payment> findByStatus(PaymentStatus status);

    boolean existsByTransactionId(String transactionId);
}
