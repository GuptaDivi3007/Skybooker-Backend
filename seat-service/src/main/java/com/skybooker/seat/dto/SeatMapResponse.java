package com.skybooker.seat.dto;

import java.util.List;

public record SeatMapResponse(
        String flightId,
        Integer totalSeats,
        Integer availableSeats,
        Integer heldSeats,
        Integer confirmedSeats,
        Integer blockedSeats,
        List<SeatResponse> seats
) {
}