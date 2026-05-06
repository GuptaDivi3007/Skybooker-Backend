package com.skybooker.booking.controller;

import com.skybooker.booking.dto.*;
import com.skybooker.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping
    public BookingResponse create(
            @RequestHeader("X-Authenticated-User-Id") String userId,
            @Valid @RequestBody BookingRequest req
    ) {
        return service.createBooking(userId, req);
    }

    @PutMapping("/{id}/confirm")
    public BookingResponse confirm(@PathVariable String id) {
        return service.confirmBooking(id);
    }

    @PutMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable String id) {
        return service.cancelBooking(id);
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable String id) {
        return service.getById(id);
    }

    @GetMapping("/pnr/{pnr}")
    public BookingResponse getByPnr(@PathVariable String pnr) {
        return service.getByPnr(pnr);
    }

    @GetMapping
    public List<BookingResponse> userBookings(
            @RequestHeader("X-Authenticated-User-Id") String userId
    ) {
        return service.getUserBookings(userId);
    }

    @GetMapping("/upcoming")
    public List<BookingResponse> upcoming(
            @RequestHeader("X-Authenticated-User-Id") String userId
    ) {
        return service.getUpcomingBookings(userId);
    }

    @GetMapping("/flight/{flightId}")
    public List<BookingResponse> flightBookings(@PathVariable String flightId) {
        return service.getFlightBookings(flightId);
    }

    @PostMapping("/fare")
    public FareSummaryResponse fare(@Valid @RequestBody FareCalculationRequest req) {
        return service.calculateFare(req);
    }

    @PutMapping("/{id}/addons")
    public BookingResponse addOns(
            @PathVariable String id,
            @RequestBody AddOnRequest req
    ) {
        return service.addAddOns(id, req);
    }

    @PutMapping("/{id}/status")
    public BookingResponse updateStatus(
            @PathVariable String id,
            @RequestBody StatusUpdateRequest req
    ) {
        return service.updateStatus(id, req);
    }
    
    @PutMapping("/{id}/payment")
    public BookingResponse linkPayment(
            @PathVariable String id,
            @Valid @RequestBody PaymentLinkRequest request) {
        return service.linkPayment(id, request);
    }
}