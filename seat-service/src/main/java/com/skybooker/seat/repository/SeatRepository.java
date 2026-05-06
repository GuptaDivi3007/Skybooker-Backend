package com.skybooker.seat.repository;

import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, String> {

    List<Seat> findByFlightId(String flightId);

    List<Seat> findByFlightIdOrderByRowNumberAscColumnLetterAsc(String flightId);

    List<Seat> findByFlightIdAndStatus(String flightId, SeatStatus status);

    List<Seat> findByFlightIdAndSeatClass(String flightId, SeatClass seatClass);

    List<Seat> findByFlightIdAndSeatClassAndStatus(String flightId, SeatClass seatClass, SeatStatus status);

    Optional<Seat> findByFlightIdAndSeatNumberIgnoreCase(String flightId, String seatNumber);

    boolean existsByFlightIdAndSeatNumberIgnoreCase(String flightId, String seatNumber);

    Long countByFlightIdAndSeatClassAndStatus(String flightId, SeatClass seatClass, SeatStatus status);

    Long countByFlightIdAndStatus(String flightId, SeatStatus status);

    Long countByFlightId(String flightId);

    List<Seat> findByStatusAndHoldExpiresAtBefore(SeatStatus status, LocalDateTime time);

    void deleteByFlightId(String flightId);
}