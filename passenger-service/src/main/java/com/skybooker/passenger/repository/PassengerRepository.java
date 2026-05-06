package com.skybooker.passenger.repository;

import com.skybooker.passenger.entity.PassengerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PassengerRepository extends JpaRepository<PassengerInfo, String> {

    List<PassengerInfo> findByBookingId(String bookingId);

    Optional<PassengerInfo> findByPassportNumberIgnoreCase(String passportNumber);

    Optional<PassengerInfo> findByTicketNumberIgnoreCase(String ticketNumber);

    Optional<PassengerInfo> findBySeatId(String seatId);

    long countByBookingId(String bookingId);

    boolean existsByTicketNumber(String ticketNumber);

    void deleteByBookingId(String bookingId);
}