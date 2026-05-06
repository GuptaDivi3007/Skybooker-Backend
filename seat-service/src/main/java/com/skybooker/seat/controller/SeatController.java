package com.skybooker.seat.controller;

import com.skybooker.seat.dto.*;
import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.exception.AccessDeniedException;
import com.skybooker.seat.service.SeatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping("/flight/{flightId}")
    public List<SeatResponse> addSeatsForFlight(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String flightId,
            @Valid @RequestBody AddSeatsRequest request) {
        requireAdminOrStaff(role);
        return seatService.addSeatsForFlight(flightId, request);
    }

    @GetMapping("/flight/{flightId}")
    public List<SeatResponse> getSeatsByFlight(@PathVariable String flightId) {
        return seatService.getSeatsByFlight(flightId);
    }

    @GetMapping("/flight/{flightId}/available")
    public List<SeatResponse> getAvailableSeats(@PathVariable String flightId) {
        return seatService.getAvailableSeats(flightId);
    }

    @GetMapping("/flight/{flightId}/class/{seatClass}")
    public List<SeatResponse> getAvailableByClass(
            @PathVariable String flightId,
            @PathVariable SeatClass seatClass) {
        return seatService.getAvailableByClass(flightId, seatClass);
    }

    @GetMapping("/flight/{flightId}/map")
    public SeatMapResponse getSeatMap(@PathVariable String flightId) {
        return seatService.getSeatMap(flightId);
    }

    @GetMapping("/flight/{flightId}/number/{seatNumber}")
    public SeatResponse getSeatByFlightAndNumber(
            @PathVariable String flightId,
            @PathVariable String seatNumber) {
        return seatService.getSeatByFlightAndSeatNumber(flightId, seatNumber);
    }

    @GetMapping("/{seatId}")
    public SeatResponse getSeatById(@PathVariable String seatId) {
        return seatService.getSeatById(seatId);
    }

    @PutMapping("/{seatId}/hold")
    public SeatResponse holdSeat(
            @RequestHeader(value = "X-Authenticated-User-Id", required = false) String userId,
            @PathVariable String seatId,
            @RequestBody(required = false) HoldSeatRequest request) {
        return seatService.holdSeat(seatId, request, userId);
    }

    @PutMapping("/{seatId}/release")
    public SeatResponse releaseSeat(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String seatId) {
        requirePassengerOrAdminOrStaffOrInternal(role);
        return seatService.releaseSeat(seatId);
    }
    
    @PutMapping("/{seatId}/cancel-confirmed")
    public SeatResponse cancelConfirmedSeat(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String seatId) {
        requireAdminOrStaffOrInternal(role);
        return seatService.cancelConfirmedSeat(seatId);
    }

    @PutMapping("/{seatId}/confirm")
    public SeatResponse confirmSeat(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String seatId) {
        requirePassengerOrAdminOrStaffOrInternal(role);
        return seatService.confirmSeat(seatId);
    }

    @PutMapping("/{seatId}")
    public SeatResponse updateSeat(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String seatId,
            @Valid @RequestBody UpdateSeatRequest request) {
        requireAdminOrStaff(role);
        return seatService.updateSeat(seatId, request);
    }

    @GetMapping("/flight/{flightId}/count/{seatClass}")
    public SeatCountResponse countAvailableByClass(
            @PathVariable String flightId,
            @PathVariable SeatClass seatClass) {
        return seatService.countAvailableByClass(flightId, seatClass);
    }

    @DeleteMapping("/flight/{flightId}")
    public MessageResponse deleteSeatsForFlight(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role,
            @PathVariable String flightId) {
        requireAdminOrStaff(role);
        return seatService.deleteSeatsForFlight(flightId);
    }

    @PutMapping("/release-expired")
    public MessageResponse releaseExpiredHolds(
            @RequestHeader(value = "X-Authenticated-User-Role", required = false) String role) {
        requireAdminOrStaffOrInternal(role);
        Integer released = seatService.releaseExpiredHolds();
        return new MessageResponse("Released expired held seats: " + released);
    }

    private void requireAdminOrStaff(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("ADMIN") && !role.equalsIgnoreCase("AIRLINE_STAFF"))) {
            throw new AccessDeniedException("Only ADMIN or AIRLINE_STAFF can perform this action");
        }
    }

    private void requireAdminOrStaffOrInternal(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("ADMIN")
                        && !role.equalsIgnoreCase("AIRLINE_STAFF")
                        && !role.equalsIgnoreCase("INTERNAL"))) {
            throw new AccessDeniedException("Only ADMIN, AIRLINE_STAFF, or INTERNAL service can perform this action");
        }
    }

    private void requirePassengerOrAdminOrStaffOrInternal(String role) {
        if (role == null ||
                (!role.equalsIgnoreCase("PASSENGER")
                        && !role.equalsIgnoreCase("ADMIN")
                        && !role.equalsIgnoreCase("AIRLINE_STAFF")
                        && !role.equalsIgnoreCase("INTERNAL"))) {
            throw new AccessDeniedException("Only authenticated users can perform this action");
        }
    }
}