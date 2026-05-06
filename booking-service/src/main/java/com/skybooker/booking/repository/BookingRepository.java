package com.skybooker.booking.repository;

import com.skybooker.booking.entity.Booking;
import com.skybooker.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, String> {

    Optional<Booking> findByPnrCode(String pnrCode);

    boolean existsByPnrCode(String pnrCode);

    List<Booking> findByUserId(String userId);

    List<Booking> findByUserIdOrderByBookedAtDesc(String userId);

    List<Booking> findByFlightId(String flightId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByUserIdAndStatus(String userId, BookingStatus status);

    long countByFlightIdAndStatus(String flightId, BookingStatus status);

    List<Booking> findByStatusAndBookedAtBefore(BookingStatus status, LocalDateTime time);

    List<Booking> findByUserIdAndStatusInOrderByBookedAtDesc(String userId, List<BookingStatus> statuses);
}