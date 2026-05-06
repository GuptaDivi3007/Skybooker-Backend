package com.skybooker.booking.dto;

import java.util.List;

public record PassengerBulkRequest(
        List<PassengerRequest> passengers
) {
}