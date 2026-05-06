package com.skybooker.booking.dto;

public record FareSummaryResponse(
        String flightId,
        Integer passengerCount,
        Double baseFare,
        Double taxes,
        Double mealCost,
        Double baggageCost,
        Double totalFare
) {
}