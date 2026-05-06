package com.skybooker.booking.dto;

import java.time.LocalDateTime;

public record SeatResponse(
        String seatId,
        String flightId,
        String seatNumber,
        String seatClass,
        Integer rowNumber,
        String columnLetter,
        boolean windowSeat,
        boolean aisleSeat,
        boolean extraLegroom,
        String status,
        Double priceMultiplier,
        String heldByUserId,
        LocalDateTime holdExpiresAt,
        Long version
) {
}