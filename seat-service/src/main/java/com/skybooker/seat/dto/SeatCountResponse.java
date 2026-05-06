package com.skybooker.seat.dto;

import com.skybooker.seat.entity.SeatClass;

public record SeatCountResponse(
        String flightId,
        SeatClass seatClass,
        Long availableCount
) {
}