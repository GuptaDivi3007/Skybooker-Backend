package com.skybooker.booking.client;

import com.skybooker.booking.dto.HoldSeatClientRequest;
import com.skybooker.booking.dto.SeatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SeatClient {

    private final RestTemplate restTemplate;

    @Value("${services.seat-service.base-url}")
    private String seatServiceBaseUrl;

    public SeatClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SeatResponse getSeatById(String seatId) {
        ResponseEntity<SeatResponse> response = restTemplate.getForEntity(
                seatServiceBaseUrl + "/seats/" + seatId,
                SeatResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Unable to fetch seat details");
        }

        return response.getBody();
    }

    public SeatResponse holdSeat(String seatId, String userId) {
        HttpHeaders headers = internalHeaders(userId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HoldSeatClientRequest body = new HoldSeatClientRequest(userId);
        HttpEntity<HoldSeatClientRequest> entity = new HttpEntity<>(body, headers);

        ResponseEntity<SeatResponse> response = restTemplate.exchange(
                seatServiceBaseUrl + "/seats/" + seatId + "/hold",
                HttpMethod.PUT,
                entity,
                SeatResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Unable to hold seat: " + seatId);
        }

        return response.getBody();
    }

    public SeatResponse releaseSeat(String seatId, String userId) {
        HttpHeaders headers = internalHeaders(userId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SeatResponse> response = restTemplate.exchange(
                seatServiceBaseUrl + "/seats/" + seatId + "/release",
                HttpMethod.PUT,
                entity,
                SeatResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Unable to release seat: " + seatId);
        }

        return response.getBody();
    }
    
    public SeatResponse cancelConfirmedSeat(String seatId, String userId) {
        HttpHeaders headers = internalHeaders(userId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SeatResponse> response = restTemplate.exchange(
                seatServiceBaseUrl + "/seats/" + seatId + "/cancel-confirmed",
                HttpMethod.PUT,
                entity,
                SeatResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Unable to cancel confirmed seat: " + seatId);
        }

        return response.getBody();
    }

    public SeatResponse confirmSeat(String seatId, String userId) {
        HttpHeaders headers = internalHeaders(userId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SeatResponse> response = restTemplate.exchange(
                seatServiceBaseUrl + "/seats/" + seatId + "/confirm",
                HttpMethod.PUT,
                entity,
                SeatResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Unable to confirm seat: " + seatId);
        }

        return response.getBody();
    }

    private HttpHeaders internalHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authenticated-User-Role", "INTERNAL");
        headers.set("X-Authenticated-User-Id", userId == null || userId.isBlank() ? "booking-service" : userId);
        headers.set("X-Authenticated-User-Email", "booking-service@internal");
        return headers;
    }
}