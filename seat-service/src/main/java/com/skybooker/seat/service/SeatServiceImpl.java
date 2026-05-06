package com.skybooker.seat.service;

import com.skybooker.seat.dto.*;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.entity.SeatStatus;
import com.skybooker.seat.exception.ResourceNotFoundException;
import com.skybooker.seat.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class SeatServiceImpl implements SeatService {

    private static final int HOLD_MINUTES = 15;

    private final SeatRepository seatRepository;

    public SeatServiceImpl(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    @Override
    public List<SeatResponse> addSeatsForFlight(String flightId, AddSeatsRequest request) {
        String normalizedFlightId = normalizeRequired(flightId, "Flight id");

        List<Seat> seats = request.seats()
                .stream()
                .map(seatRequest -> createSeatEntity(normalizedFlightId, seatRequest))
                .toList();

        List<Seat> savedSeats = seatRepository.saveAll(seats);

        return savedSeats.stream().map(this::mapToResponse).toList();
    }

    @Override
    public List<SeatResponse> getSeatsByFlight(String flightId) {
        return seatRepository.findByFlightIdOrderByRowNumberAscColumnLetterAsc(flightId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<SeatResponse> getAvailableSeats(String flightId) {
        return seatRepository.findByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<SeatResponse> getAvailableByClass(String flightId, SeatClass seatClass) {
        return seatRepository.findByFlightIdAndSeatClassAndStatus(flightId, seatClass, SeatStatus.AVAILABLE)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public SeatResponse getSeatById(String seatId) {
        return mapToResponse(findSeat(seatId));
    }

    @Override
    public SeatResponse getSeatByFlightAndSeatNumber(String flightId, String seatNumber) {
        Seat seat = seatRepository
                .findByFlightIdAndSeatNumberIgnoreCase(flightId, normalizeRequired(seatNumber, "Seat number"))
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found for flight " + flightId + " and seat number " + seatNumber));

        return mapToResponse(seat);
    }

    @Override
    public SeatResponse holdSeat(String seatId, HoldSeatRequest request, String authenticatedUserId) {
        Seat seat = findSeat(seatId);
        String userId = getUserIdForHold(request, authenticatedUserId);

        if (seat.getStatus() == SeatStatus.HELD) {
            boolean expired = seat.getHoldExpiresAt() != null && seat.getHoldExpiresAt().isBefore(LocalDateTime.now());

            if (expired) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setHeldByUserId(null);
                seat.setHoldExpiresAt(null);
            } else if (Objects.equals(seat.getHeldByUserId(), userId)) {
                seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES));
                return mapToResponse(seatRepository.save(seat));
            }
        }

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new IllegalArgumentException("Seat is not available. Current status: " + seat.getStatus());
        }

        seat.setStatus(SeatStatus.HELD);
        seat.setHeldByUserId(userId);
        seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES));

        return mapToResponse(seatRepository.save(seat));
    }

    @Override
    public SeatResponse releaseSeat(String seatId) {
        Seat seat = findSeat(seatId);

        if (seat.getStatus() == SeatStatus.CONFIRMED) {
            throw new IllegalArgumentException("Confirmed seat cannot be released directly");
        }

        if (seat.getStatus() == SeatStatus.BLOCKED) {
            throw new IllegalArgumentException("Blocked seat cannot be released using release API. Update the seat status instead.");
        }

        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setHeldByUserId(null);
        seat.setHoldExpiresAt(null);

        return mapToResponse(seatRepository.save(seat));
    }
    
    @Override
    public SeatResponse cancelConfirmedSeat(String seatId) {
        Seat seat = findSeat(seatId);

        if (seat.getStatus() != SeatStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only CONFIRMED seat can be cancelled using this API");
        }

        seat.setStatus(SeatStatus.AVAILABLE);
        seat.setHeldByUserId(null);
        seat.setHoldExpiresAt(null);

        return mapToResponse(seatRepository.save(seat));
    }

    @Override
    public SeatResponse confirmSeat(String seatId) {
        Seat seat = findSeat(seatId);

        if (seat.getStatus() != SeatStatus.HELD) {
            throw new IllegalArgumentException("Only HELD seat can be confirmed. Current status: " + seat.getStatus());
        }

        if (seat.getHoldExpiresAt() != null && seat.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldByUserId(null);
            seat.setHoldExpiresAt(null);
            seatRepository.save(seat);
            throw new IllegalArgumentException("Seat hold expired. Please hold the seat again.");
        }

        seat.setStatus(SeatStatus.CONFIRMED);
        seat.setHoldExpiresAt(null);

        return mapToResponse(seatRepository.save(seat));
    }

    @Override
    public SeatResponse updateSeat(String seatId, UpdateSeatRequest request) {
        Seat seat = findSeat(seatId);

        if (request.seatNumber() != null && !request.seatNumber().isBlank()) {
            String newSeatNumber = normalizeSeatNumber(request.seatNumber());

            if (!seat.getSeatNumber().equalsIgnoreCase(newSeatNumber)
                    && seatRepository.existsByFlightIdAndSeatNumberIgnoreCase(seat.getFlightId(), newSeatNumber)) {
                throw new IllegalArgumentException("Seat number already exists for this flight: " + newSeatNumber);
            }

            seat.setSeatNumber(newSeatNumber);
        }

        if (request.seatClass() != null) {
            seat.setSeatClass(request.seatClass());
        }

        if (request.rowNumber() != null) {
            seat.setRowNumber(request.rowNumber());
        }

        if (request.columnLetter() != null && !request.columnLetter().isBlank()) {
            seat.setColumnLetter(request.columnLetter().trim().toUpperCase());
        }

        if (request.windowSeat() != null) {
            seat.setWindowSeat(request.windowSeat());
        }

        if (request.aisleSeat() != null) {
            seat.setAisleSeat(request.aisleSeat());
        }

        if (request.extraLegroom() != null) {
            seat.setExtraLegroom(request.extraLegroom());
        }

        if (request.priceMultiplier() != null) {
            seat.setPriceMultiplier(request.priceMultiplier());
        }

        if (request.status() != null) {
            applyManualStatusUpdate(seat, request.status());
        }

        return mapToResponse(seatRepository.save(seat));
    }

    @Override
    public SeatMapResponse getSeatMap(String flightId) {
        List<SeatResponse> seats = getSeatsByFlight(flightId);

        Integer totalSeats = seats.size();
        Integer availableSeats = Math.toIntExact(seatRepository.countByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE));
        Integer heldSeats = Math.toIntExact(seatRepository.countByFlightIdAndStatus(flightId, SeatStatus.HELD));
        Integer confirmedSeats = Math.toIntExact(seatRepository.countByFlightIdAndStatus(flightId, SeatStatus.CONFIRMED));
        Integer blockedSeats = Math.toIntExact(seatRepository.countByFlightIdAndStatus(flightId, SeatStatus.BLOCKED));

        return new SeatMapResponse(
                flightId,
                totalSeats,
                availableSeats,
                heldSeats,
                confirmedSeats,
                blockedSeats,
                seats
        );
    }

    @Override
    public SeatCountResponse countAvailableByClass(String flightId, SeatClass seatClass) {
        Long count = seatRepository.countByFlightIdAndSeatClassAndStatus(flightId, seatClass, SeatStatus.AVAILABLE);
        return new SeatCountResponse(flightId, seatClass, count);
    }

    @Override
    public MessageResponse deleteSeatsForFlight(String flightId) {
        Long count = seatRepository.countByFlightId(flightId);
        seatRepository.deleteByFlightId(flightId);
        return new MessageResponse("Deleted " + count + " seats for flight " + flightId);
    }

    @Override
    public Integer releaseExpiredHolds() {
        List<Seat> expiredHeldSeats = seatRepository.findByStatusAndHoldExpiresAtBefore(
                SeatStatus.HELD,
                LocalDateTime.now()
        );

        for (Seat seat : expiredHeldSeats) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldByUserId(null);
            seat.setHoldExpiresAt(null);
        }

        seatRepository.saveAll(expiredHeldSeats);

        return expiredHeldSeats.size();
    }

    private Seat createSeatEntity(String flightId, SeatRequest request) {
        String seatNumber = normalizeSeatNumber(request.seatNumber());

        if (seatRepository.existsByFlightIdAndSeatNumberIgnoreCase(flightId, seatNumber)) {
            throw new IllegalArgumentException("Seat already exists for flight " + flightId + ": " + seatNumber);
        }

        Seat seat = new Seat();
        seat.setFlightId(flightId);
        seat.setSeatNumber(seatNumber);
        seat.setSeatClass(request.seatClass());
        seat.setRowNumber(request.rowNumber());
        seat.setColumnLetter(normalizeRequired(request.columnLetter(), "Column letter"));
        seat.setWindowSeat(request.windowSeat());
        seat.setAisleSeat(request.aisleSeat());
        seat.setExtraLegroom(request.extraLegroom());
        seat.setPriceMultiplier(request.priceMultiplier());
        seat.setStatus(request.status() == null ? SeatStatus.AVAILABLE : request.status());
        seat.setHeldByUserId(null);
        seat.setHoldExpiresAt(null);

        if (seat.getStatus() == SeatStatus.HELD) {
            seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES));
        }

        return seat;
    }

    private Seat findSeat(String seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found with id: " + seatId));
    }

    private String getUserIdForHold(HoldSeatRequest request, String authenticatedUserId) {
        if (request != null && request.userId() != null && !request.userId().isBlank()) {
            return request.userId().trim();
        }

        if (authenticatedUserId != null && !authenticatedUserId.isBlank()) {
            return authenticatedUserId.trim();
        }

        return null;
    }

    private void applyManualStatusUpdate(Seat seat, SeatStatus newStatus) {
        if (newStatus == SeatStatus.AVAILABLE) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setHeldByUserId(null);
            seat.setHoldExpiresAt(null);
            return;
        }

        if (newStatus == SeatStatus.BLOCKED) {
            seat.setStatus(SeatStatus.BLOCKED);
            seat.setHeldByUserId(null);
            seat.setHoldExpiresAt(null);
            return;
        }

        if (newStatus == SeatStatus.HELD) {
            if (seat.getStatus() == SeatStatus.CONFIRMED) {
                throw new IllegalArgumentException("Confirmed seat cannot be moved to HELD manually");
            }
            seat.setStatus(SeatStatus.HELD);
            seat.setHoldExpiresAt(LocalDateTime.now().plusMinutes(HOLD_MINUTES));
            return;
        }

        if (newStatus == SeatStatus.CONFIRMED) {
            seat.setStatus(SeatStatus.CONFIRMED);
            seat.setHoldExpiresAt(null);
        }
    }

    private SeatResponse mapToResponse(Seat seat) {
        return new SeatResponse(
                seat.getSeatId(),
                seat.getFlightId(),
                seat.getSeatNumber(),
                seat.getSeatClass(),
                seat.getRowNumber(),
                seat.getColumnLetter(),
                seat.isWindowSeat(),
                seat.isAisleSeat(),
                seat.isExtraLegroom(),
                seat.getStatus(),
                seat.getPriceMultiplier(),
                seat.getHeldByUserId(),
                seat.getHoldExpiresAt(),
                seat.getVersion()
        );
    }

    private String normalizeSeatNumber(String value) {
        return normalizeRequired(value, "Seat number").toUpperCase();
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim().toUpperCase();
    }
}
