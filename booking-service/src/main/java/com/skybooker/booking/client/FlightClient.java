package com.skybooker.booking.client;

import com.skybooker.booking.dto.FlightResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FlightClient {

    private final RestTemplate restTemplate;

    @Value("${services.flight-service.base-url}")
    private String flightServiceBaseUrl;

    public FlightClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public FlightResponse getFlightById(String flightId) {
        ResponseEntity<FlightResponse> response = restTemplate.getForEntity(
                flightServiceBaseUrl + "/flights/" + flightId,
                FlightResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Unable to fetch flight details");
        }

        return response.getBody();
    }

    public void decrementSeats(String flightId, Integer seats) {
        HttpEntity<Void> entity = new HttpEntity<>(internalHeaders());

        restTemplate.exchange(
                flightServiceBaseUrl + "/flights/" + flightId + "/decrement-seats?seats=" + seats,
                HttpMethod.PUT,
                entity,
                Void.class
        );
    }

    public void incrementSeats(String flightId, Integer seats) {
        HttpEntity<Void> entity = new HttpEntity<>(internalHeaders());

        restTemplate.exchange(
                flightServiceBaseUrl + "/flights/" + flightId + "/increment-seats?seats=" + seats,
                HttpMethod.PUT,
                entity,
                Void.class
        );
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authenticated-User-Role", "INTERNAL");
        headers.set("X-Authenticated-User-Id", "booking-service");
        headers.set("X-Authenticated-User-Email", "booking-service@internal");
        return headers;
    }
}