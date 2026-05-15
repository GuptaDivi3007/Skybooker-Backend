package com.skybooker.seat.service;

import com.skybooker.seat.dto.AddSeatsRequest;
import com.skybooker.seat.dto.SeatRequest;
import com.skybooker.seat.dto.SeatResponse;
import com.skybooker.seat.entity.Seat;
import com.skybooker.seat.entity.SeatClass;
import com.skybooker.seat.entity.SeatStatus;
import com.skybooker.seat.exception.ResourceNotFoundException;
import com.skybooker.seat.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    @Mock
    private SeatRepository seatRepository;

    private SeatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SeatServiceImpl(seatRepository);
    }

    @Test
    void addSeatsForFlightCreatesAvailableSeats() {
        when(seatRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SeatResponse> response = service.addSeatsForFlight(" flight-1 ", new AddSeatsRequest(List.of(
                new SeatRequest(" 1a ", SeatClass.ECONOMY, 1, " a ", true, false, false, 1.0, null)
        )));

        assertEquals(1, response.size());
        assertEquals("1A", response.get(0).seatNumber());
        assertEquals(SeatStatus.AVAILABLE, response.get(0).status());
    }

    @Test
    void addSeatsForFlightRejectsDuplicateSeatNumber() {
        when(seatRepository.existsByFlightIdAndSeatNumberIgnoreCase("FLIGHT-1", "1A")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.addSeatsForFlight("flight-1", new AddSeatsRequest(List.of(
                new SeatRequest("1A", SeatClass.ECONOMY, 1, "A", true, false, false, 1.0, null)
        ))));
    }

    @Test
    void holdSeatMarksAvailableSeatAsHeld() {
        Seat seat = seat("seat-1", SeatStatus.AVAILABLE);
        when(seatRepository.findById("seat-1")).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeatResponse response = service.holdSeat("seat-1", null, "user-1");

        assertEquals(SeatStatus.HELD, response.status());
        assertEquals("user-1", response.heldByUserId());
        assertNotNull(response.holdExpiresAt());
    }

    @Test
    void releaseSeatRejectsConfirmedSeat() {
        when(seatRepository.findById("seat-1")).thenReturn(Optional.of(seat("seat-1", SeatStatus.CONFIRMED)));

        assertThrows(IllegalArgumentException.class, () -> service.releaseSeat("seat-1"));
    }

    @Test
    void releaseExpiredHoldsClearsHeldSeats() {
        Seat expired = seat("seat-1", SeatStatus.HELD);
        expired.setHeldByUserId("user-1");
        expired.setHoldExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(seatRepository.findByStatusAndHoldExpiresAtBefore(eq(SeatStatus.HELD), any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        Integer released = service.releaseExpiredHolds();

        assertEquals(1, released);
        assertEquals(SeatStatus.AVAILABLE, expired.getStatus());
        assertNull(expired.getHeldByUserId());
        verify(seatRepository).saveAll(List.of(expired));
    }

    @Test
    void getSeatByIdThrowsWhenMissing() {
        when(seatRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getSeatById("missing"));
    }

    private Seat seat(String id, SeatStatus status) {
        Seat seat = new Seat();
        seat.setSeatId(id);
        seat.setFlightId("flight-1");
        seat.setSeatNumber("1A");
        seat.setSeatClass(SeatClass.ECONOMY);
        seat.setRowNumber(1);
        seat.setColumnLetter("A");
        seat.setWindowSeat(true);
        seat.setAisleSeat(false);
        seat.setExtraLegroom(false);
        seat.setPriceMultiplier(1.0);
        seat.setStatus(status);
        return seat;
    }
}
