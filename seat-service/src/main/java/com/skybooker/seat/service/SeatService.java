package com.skybooker.seat.service;

import com.skybooker.seat.dto.*;
import com.skybooker.seat.entity.SeatClass;

import java.util.List;

public interface SeatService {

    List<SeatResponse> addSeatsForFlight(String flightId, AddSeatsRequest request);

    List<SeatResponse> getSeatsByFlight(String flightId);

    List<SeatResponse> getAvailableSeats(String flightId);

    List<SeatResponse> getAvailableByClass(String flightId, SeatClass seatClass);

    SeatResponse getSeatById(String seatId);

    SeatResponse getSeatByFlightAndSeatNumber(String flightId, String seatNumber);

    SeatResponse holdSeat(String seatId, HoldSeatRequest request, String authenticatedUserId);

    SeatResponse releaseSeat(String seatId);
    
    SeatResponse cancelConfirmedSeat(String seatId);

    SeatResponse confirmSeat(String seatId);

    SeatResponse updateSeat(String seatId, UpdateSeatRequest request);

    SeatMapResponse getSeatMap(String flightId);

    SeatCountResponse countAvailableByClass(String flightId, SeatClass seatClass);

    MessageResponse deleteSeatsForFlight(String flightId);

    Integer releaseExpiredHolds();
}