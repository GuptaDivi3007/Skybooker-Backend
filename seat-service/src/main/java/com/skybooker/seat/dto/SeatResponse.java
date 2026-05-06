package com.skybooker.seat.dto;

import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.entity.SeatStatus;

import java.time.LocalDateTime;

public record SeatResponse(
        String seatId,
        String flightId,
        String seatNumber,
        SeatClass seatClass,
        Integer rowNumber,
        String columnLetter,
        boolean windowSeat,
        boolean aisleSeat,
        boolean extraLegroom,
        SeatStatus status,
        Double priceMultiplier,
        String heldByUserId,
        LocalDateTime holdExpiresAt,
        Long version
) {
}