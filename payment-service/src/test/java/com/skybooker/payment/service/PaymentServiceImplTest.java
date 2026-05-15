package com.skybooker.payment.service;

import com.razorpay.RazorpayException;
import com.skybooker.payment.client.BookingClient;
import com.skybooker.payment.dto.*;
import com.skybooker.payment.entity.Payment;
import com.skybooker.payment.entity.PaymentMethod;
import com.skybooker.payment.entity.PaymentStatus;
import com.skybooker.payment.exception.BadRequestException;
import com.skybooker.payment.exception.ResourceNotFoundException;
import com.skybooker.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingClient bookingClient;

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() throws RazorpayException {
        service = new PaymentServiceImpl(paymentRepository, bookingClient, "key", "secret");
        ReflectionTestUtils.setField(service, "currency", "INR");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "secret");
    }

    @Test
    void createPaymentSavesPendingPayment() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.createPayment(" user-1 ", new PaymentRequest(" booking-1 ", 1200.0));

        assertEquals("user-1", response.userId());
        assertEquals("booking-1", response.bookingId());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertEquals("INR", response.currency());
    }

    @Test
    void processPaymentMarksPaymentSuccessAndConfirmsBooking() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(paymentRepository.findById("payment-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.existsByTransactionId(any())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.processPayment("payment-1", new ProcessPaymentRequest("UPI"));

        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertEquals("UPI", response.paymentMethod());
        verify(bookingClient).linkPayment("booking-1", "payment-1");
        verify(bookingClient).confirmBooking("booking-1");
    }

    @Test
    void processPaymentRejectsNonPendingPayment() {
        when(paymentRepository.findById("payment-1")).thenReturn(Optional.of(payment(PaymentStatus.SUCCESS)));

        assertThrows(BadRequestException.class, () -> service.processPayment("payment-1", new ProcessPaymentRequest("UPI")));
    }

    @Test
    void failPaymentCancelsBooking() {
        Payment payment = payment(PaymentStatus.PENDING);
        when(paymentRepository.findById("payment-1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.failPayment("payment-1", new PaymentFailureRequest("Card declined"));

        assertEquals(PaymentStatus.FAILED, response.status());
        verify(bookingClient).cancelBooking("booking-1");
    }

    @Test
    void getMyPaymentsMapsRepositoryPayments() {
        when(paymentRepository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(payment(PaymentStatus.SUCCESS)));

        List<PaymentResponse> response = service.getMyPayments("user-1");

        assertEquals(1, response.size());
        assertEquals(PaymentStatus.SUCCESS, response.get(0).status());
    }

    @Test
    void getPaymentByIdThrowsWhenMissing() {
        when(paymentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getPaymentById("missing"));
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setPaymentId("payment-1");
        payment.setBookingId("booking-1");
        payment.setUserId("user-1");
        payment.setAmount(1200.0);
        payment.setCurrency("INR");
        payment.setStatus(status);
        payment.setPaymentMethod(PaymentMethod.UPI);
        return payment;
    }
}
