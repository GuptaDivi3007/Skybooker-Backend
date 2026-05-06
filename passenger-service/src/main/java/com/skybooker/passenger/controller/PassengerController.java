package com.skybooker.passenger.controller;

import com.skybooker.passenger.dto.*;
import com.skybooker.passenger.exception.AccessDeniedException;
import com.skybooker.passenger.service.PassengerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/passengers")
public class PassengerController {

    private final PassengerService passengerService;

    public PassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    @PostMapping
    public PassengerResponse addPassenger(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @Valid @RequestBody PassengerRequest request) {
        requirePassengerAdminOrInternal(role);
        return passengerService.addPassenger(request);
    }

    @PostMapping("/bulk")
    public List<PassengerResponse> addPassengers(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @Valid @RequestBody PassengerBulkRequest request) {
        requirePassengerAdminOrInternal(role);
        return passengerService.addPassengers(request);
    }

    @GetMapping("/{passengerId}")
    public PassengerResponse getPassengerById(@PathVariable String passengerId) {
        return passengerService.getPassengerById(passengerId);
    }

    @GetMapping("/booking/{bookingId}")
    public List<PassengerResponse> getPassengersByBooking(@PathVariable String bookingId) {
        return passengerService.getPassengersByBooking(bookingId);
    }

    @GetMapping("/passport/{passportNumber}")
    public PassengerResponse getByPassportNumber(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String passportNumber) {
        requireAdminStaffOrInternal(role);
        return passengerService.getByPassportNumber(passportNumber);
    }

    @GetMapping("/ticket/{ticketNumber}")
    public PassengerResponse getByTicketNumber(@PathVariable String ticketNumber) {
        return passengerService.getByTicketNumber(ticketNumber);
    }

    @PutMapping("/{passengerId}")
    public PassengerResponse updatePassenger(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String passengerId,
            @Valid @RequestBody UpdatePassengerRequest request) {
        requirePassengerAdminOrInternal(role);
        return passengerService.updatePassenger(passengerId, request);
    }

    @PutMapping("/{passengerId}/assign-seat")
    public PassengerResponse assignSeat(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String passengerId,
            @Valid @RequestBody AssignSeatRequest request) {
        requirePassengerAdminStaffOrInternal(role);
        return passengerService.assignSeat(passengerId, request);
    }

    @PutMapping("/{passengerId}/check-in")
    public PassengerResponse checkIn(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String passengerId,
            @Valid @RequestBody CheckInRequest request) {
        requirePassengerAdminStaffOrInternal(role);
        return passengerService.checkIn(passengerId, request);
    }

    @DeleteMapping("/{passengerId}")
    public MessageResponse deletePassenger(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String passengerId) {
        requireAdminOrInternal(role);
        return passengerService.deletePassenger(passengerId);
    }

    @DeleteMapping("/booking/{bookingId}")
    public MessageResponse deletePassengersByBooking(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String bookingId) {
        requireAdminOrInternal(role);
        return passengerService.deletePassengersByBooking(bookingId);
    }

    @GetMapping("/booking/{bookingId}/count")
    public Long getPassengerCount(@PathVariable String bookingId) {
        return passengerService.getPassengerCount(bookingId);
    }

    private void requirePassengerAdminOrInternal(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("PASSENGER")
                        && !role.equalsIgnoreCase("ADMIN")
                        && !role.equalsIgnoreCase("INTERNAL"))) {
            throw new AccessDeniedException("Only PASSENGER, ADMIN, or INTERNAL can perform this action");
        }
    }

    private void requirePassengerAdminStaffOrInternal(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("PASSENGER")
                        && !role.equalsIgnoreCase("ADMIN")
                        && !role.equalsIgnoreCase("AIRLINE_STAFF")
                        && !role.equalsIgnoreCase("INTERNAL"))) {
            throw new AccessDeniedException("Only authenticated users can perform this action");
        }
    }

    private void requireAdminStaffOrInternal(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("ADMIN")
                        && !role.equalsIgnoreCase("AIRLINE_STAFF")
                        && !role.equalsIgnoreCase("INTERNAL"))) {
            throw new AccessDeniedException("Only ADMIN, AIRLINE_STAFF, or INTERNAL can perform this action");
        }
    }

    private void requireAdminOrInternal(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("ADMIN")
                        && !role.equalsIgnoreCase("INTERNAL"))) {
            throw new AccessDeniedException("Only ADMIN or INTERNAL can perform this action");
        }
    }
}