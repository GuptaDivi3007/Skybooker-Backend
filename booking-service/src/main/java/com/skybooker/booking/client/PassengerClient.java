package com.skybooker.booking.client;

import com.skybooker.booking.dto.PassengerBulkRequest;
import com.skybooker.booking.dto.PassengerRequest;
import com.skybooker.booking.dto.PassengerResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class PassengerClient {

    private final RestTemplate restTemplate;

    @Value("${services.passenger-service.base-url}")
    private String passengerServiceBaseUrl;

    public PassengerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void addPassengers(String bookingId, List<PassengerRequest> passengers) {

        List<PassengerRequest> passengerRequests = passengers.stream()
                .map(p -> new PassengerRequest(
                        bookingId,
                        p.title(),
                        p.firstName(),
                        p.lastName(),
                        p.dateOfBirth(),
                        p.gender(),
                        p.passportNumber(),
                        p.nationality(),
                        p.passportExpiry(),
                        p.passengerType()
                ))
                .toList();

        PassengerBulkRequest body = new PassengerBulkRequest(passengerRequests);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Authenticated-User-Role", "INTERNAL");
        headers.set("X-Authenticated-User-Id", "booking-service");
        headers.set("X-Authenticated-User-Email", "booking-service@internal");

        HttpEntity<PassengerBulkRequest> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange(
                passengerServiceBaseUrl + "/passengers/bulk",
                HttpMethod.POST,
                entity,
                Object.class
        );
    }

    public List<PassengerResponse> getPassengersByBooking(String bookingId) {
        ResponseEntity<List<PassengerResponse>> response = restTemplate.exchange(
                passengerServiceBaseUrl + "/passengers/booking/" + bookingId,
                HttpMethod.GET,
                new HttpEntity<>(internalHeaders()),
                new ParameterizedTypeReference<>() {
                }
        );

        return response.getBody() == null ? List.of() : response.getBody();
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authenticated-User-Role", "INTERNAL");
        headers.set("X-Authenticated-User-Id", "booking-service");
        headers.set("X-Authenticated-User-Email", "booking-service@internal");
        return headers;
    }
}
