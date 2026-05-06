package com.skybooker.booking.service;

import com.skybooker.booking.client.FlightClient;
import com.skybooker.booking.client.PassengerClient;
import com.skybooker.booking.client.PaymentClient;
import com.skybooker.booking.client.SeatClient;
import com.skybooker.booking.dto.*;
import com.skybooker.booking.entity.*;
import com.skybooker.booking.exception.BadRequestException;
import com.skybooker.booking.exception.ResourceNotFoundException;
import com.skybooker.booking.producer.BookingNotificationProducer;
import com.skybooker.booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository repo;
    private final FlightClient flightClient;
    private final SeatClient seatClient;
    private final BookingNotificationProducer notificationProducer;
    private final PaymentClient paymentClient;
    private final PassengerClient passengerClient;

    @Value("${booking.tax-rate}")
    private double taxRate;

    @Value("${booking.meal-price}")
    private double mealPrice;

    @Value("${booking.baggage-price-per-kg}")
    private double baggagePrice;

    public BookingServiceImpl(BookingRepository repo,
                              FlightClient flightClient,
                              SeatClient seatClient,
                              BookingNotificationProducer notificationProducer,
                              PaymentClient paymentClient,
                              PassengerClient passengerClient) {
        this.repo = repo;
        this.flightClient = flightClient;
        this.seatClient = seatClient;
        this.notificationProducer = notificationProducer;
        this.paymentClient = paymentClient;
        this.passengerClient = passengerClient;
    }

    @Override
    public BookingResponse createBooking(String userId, BookingRequest req) {
    	// Seat validation
        if (req.seatIds().size() != req.passengerCount()) {
            throw new BadRequestException("Seat count must match passenger count");
        }
        
        if (req.passengers() == null || req.passengers().size() != req.passengerCount()) {
            throw new BadRequestException("Passenger details count must match passenger count");
        }

        FlightResponse flight = flightClient.getFlightById(req.flightId());

        if (flight.availableSeats() < req.passengerCount()) {
            throw new BadRequestException("Not enough seats available");
        }

        List<String> heldSeats = new ArrayList<>();

        try {
            for (String seatId : req.seatIds()) {
                seatClient.holdSeat(seatId, userId);
                heldSeats.add(seatId);
            }
        } catch (Exception e) {
            for (String seatId : heldSeats) {
                seatClient.releaseSeat(seatId, userId);
            }
            throw new RuntimeException("Seat hold failed");
        }

        FareSummaryResponse fare = calculateFareInternal(flight, req);

        Booking b = new Booking();
        b.setUserId(userId);
        b.setFlightId(req.flightId());
        b.setSeatIds(req.seatIds());
        b.setTripType(req.tripType());
        b.setPassengerCount(req.passengerCount());
        b.setMealPreference(req.mealPreference());
        b.setLuggageKg(req.luggageKg());
        b.setContactEmail(req.contactEmail());
        b.setContactPhone(req.contactPhone());
        b.setBaseFare(fare.baseFare());
        b.setTaxes(fare.taxes());
        b.setMealCost(fare.mealCost());
        b.setBaggageCost(fare.baggageCost());
        b.setTotalFare(fare.totalFare());
        b.setStatus(BookingStatus.PENDING);
        b.setPnrCode(generatePNR());
        b.setBookedAt(LocalDateTime.now());

        Booking saved = repo.save(b);

        try {
            passengerClient.addPassengers(saved.getBookingId(), req.passengers());
        } catch (Exception e) {
            for (String seatId : saved.getSeatIds()) {
                seatClient.releaseSeat(seatId, saved.getUserId());
            }
            saved.setStatus(BookingStatus.CANCELLED);
            repo.save(saved);
            throw new RuntimeException("Passenger creation failed. Booking cancelled.");
        }

        paymentClient.createPayment(userId, saved.getBookingId(), saved.getTotalFare());

        return map(saved);
    }

    @Override
    public BookingResponse confirmBooking(String bookingId) {
        Booking b = find(bookingId);

        if (b.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only PENDING booking can be confirmed");
        }

        if (b.getSeatIds() == null || b.getSeatIds().isEmpty()) {
            throw new BadRequestException("No seats found for booking");
        }

        for (String seatId : b.getSeatIds()) {
            seatClient.confirmSeat(seatId, b.getUserId());
        }

        flightClient.decrementSeats(b.getFlightId(), b.getSeatIds().size());

        b.setStatus(BookingStatus.CONFIRMED);
        Booking saved = repo.save(b);

        notificationProducer.sendBookingConfirmed(buildBookingNotificationEvent(saved, "BOOKING_CONFIRMED"));

        return map(saved);
    }

    @Override
    public BookingResponse cancelBooking(String bookingId) {
        Booking b = find(bookingId);

        if (b.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Already cancelled");
        }

        if (b.getStatus() == BookingStatus.PENDING) {
            for (String seatId : b.getSeatIds()) {
                seatClient.releaseSeat(seatId, b.getUserId());
            }
        }

        if (b.getStatus() == BookingStatus.CONFIRMED) {
            for (String seatId : b.getSeatIds()) {
                seatClient.cancelConfirmedSeat(seatId, b.getUserId());
            }
            flightClient.incrementSeats(b.getFlightId(), b.getSeatIds().size());
        }

        b.setStatus(BookingStatus.CANCELLED);
        Booking saved = repo.save(b);

        notificationProducer.sendBookingCancelled(buildBookingNotificationEvent(saved, "CANCELLATION"));

        return map(saved);
    }

    @Override
    public BookingResponse getById(String id) {
        return map(find(id));
    }

    @Override
    public BookingResponse getByPnr(String pnr) {
        return map(repo.findByPnrCode(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("PNR not found")));
    }

    @Override
    public List<BookingResponse> getUserBookings(String userId) {
        return repo.findByUserIdOrderByBookedAtDesc(userId).stream().map(this::map).toList();
    }

    @Override
    public List<BookingResponse> getUpcomingBookings(String userId) {
        return repo.findByUserIdAndStatusInOrderByBookedAtDesc(
                userId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING)
        ).stream().map(this::map).toList();
    }

    @Override
    public List<BookingResponse> getFlightBookings(String flightId) {
        return repo.findByFlightId(flightId).stream().map(this::map).toList();
    }

    @Override
    public FareSummaryResponse calculateFare(FareCalculationRequest req) {
        FlightResponse flight = flightClient.getFlightById(req.flightId());
        return calculateFareInternal(flight, req);
    }

    private FareSummaryResponse calculateFareInternal(FlightResponse flight, BookingRequest req) {
        return calculateFareInternal(flight,
                new FareCalculationRequest(
                        req.flightId(),
                        req.passengerCount(),
                        req.mealPreference(),
                        req.luggageKg()
                ));
    }

    private FareSummaryResponse calculateFareInternal(FlightResponse flight, FareCalculationRequest req) {
        double base = flight.basePrice() * req.passengerCount();
        double tax = base * taxRate;
        double meal = (req.mealPreference() != null && req.mealPreference() != MealPreference.NONE)
                ? mealPrice * req.passengerCount()
                : 0;
        double baggage = (req.luggageKg() != null) ? req.luggageKg() * baggagePrice : 0;
        double total = base + tax + meal + baggage;

        return new FareSummaryResponse(req.flightId(), req.passengerCount(), base, tax, meal, baggage, total);
    }

    @Override
    public BookingResponse addAddOns(String bookingId, AddOnRequest req) {
        Booking b = find(bookingId);
        b.setMealPreference(req.mealPreference());
        b.setLuggageKg(req.luggageKg());

        FareSummaryResponse fare = calculateFareInternal(
                flightClient.getFlightById(b.getFlightId()),
                new FareCalculationRequest(b.getFlightId(), b.getPassengerCount(), req.mealPreference(), req.luggageKg())
        );

        b.setMealCost(fare.mealCost());
        b.setBaggageCost(fare.baggageCost());
        b.setTotalFare(fare.totalFare());

        return map(repo.save(b));
    }

    @Override
    public BookingResponse updateStatus(String id, StatusUpdateRequest req) {
        Booking b = find(id);
        b.setStatus(req.status());
        Booking saved = repo.save(b);

        if (req.status() == BookingStatus.CONFIRMED) {
            notificationProducer.sendBookingConfirmed(buildBookingNotificationEvent(saved, "BOOKING_CONFIRMED"));
        } else if (req.status() == BookingStatus.CANCELLED) {
            notificationProducer.sendBookingCancelled(buildBookingNotificationEvent(saved, "CANCELLATION"));
        } else {
            notificationProducer.sendBookingStatusChanged(buildBookingNotificationEvent(saved, "SYSTEM_ALERT"));
        }

        return map(saved);
    }

    @Override
    public BookingResponse linkPayment(String bookingId, PaymentLinkRequest request) {
        Booking booking = find(bookingId);

        if (!booking.getBookingId().equals(request.bookingId())) {
            throw new BadRequestException("Booking id mismatch");
        }

        booking.setPaymentId(request.paymentId());
        return map(repo.save(booking));
    }

    private Booking find(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    private NotificationEvent buildBookingNotificationEvent(Booking b, String notificationType) {
        FlightResponse flight = flightClient.getFlightById(b.getFlightId());
        List<PassengerResponse> passengers = safePassengers(b.getBookingId());
        String passengerNames = passengers.isEmpty()
                ? b.getPassengerCount() + " passenger(s)"
                : passengers.stream()
                .map(p -> String.join(" ",
                        nullToBlank(p.title()),
                        nullToBlank(p.firstName()),
                        nullToBlank(p.lastName())).trim())
                .filter(name -> !name.isBlank())
                .collect(Collectors.joining(", "));

        String title = switch (notificationType) {
            case "BOOKING_CONFIRMED" -> "Booking Confirmed";
            case "CANCELLATION" -> "Booking Cancelled";
            default -> "Booking Status Updated";
        };

        String message = switch (notificationType) {
            case "BOOKING_CONFIRMED" -> "Your booking is confirmed. PNR: " + b.getPnrCode();
            case "CANCELLATION" -> "Your booking has been cancelled. PNR: " + b.getPnrCode();
            default -> "Your booking status is " + b.getStatus() + ". PNR: " + b.getPnrCode();
        };

        return new NotificationEvent(
                "BOOKING_EVENT",
                notificationType,
                b.getUserId(),
                b.getContactEmail(),
                b.getContactPhone(),
                title,
                message,
                b.getBookingId(),
                b.getPnrCode(),
                b.getPaymentId(),
                flight.flightNumber(),
                flight.airlineId(),
                flight.originAirportCode(),
                flight.destinationAirportCode(),
                flight.departureTime(),
                flight.arrivalTime(),
                flight.durationMinutes(),
                flight.aircraftType(),
                String.join(", ", b.getSeatIds()),
                passengerNames,
                b.getPassengerCount(),
                b.getBaseFare(),
                b.getTaxes(),
                b.getMealCost(),
                b.getBaggageCost(),
                b.getTotalFare(),
                b.getStatus().name(),
                LocalDateTime.now()
        );
    }

    private List<PassengerResponse> safePassengers(String bookingId) {
        try {
            return passengerClient.getPassengersByBooking(bookingId);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String generatePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder pnr = new StringBuilder();
        Random r = new Random();

        for (int i = 0; i < 6; i++) {
            pnr.append(chars.charAt(r.nextInt(chars.length())));
        }

        if (repo.existsByPnrCode(pnr.toString())) {
            return generatePNR();
        }
        return pnr.toString();
    }

    private BookingResponse map(Booking b) {
        return new BookingResponse(
                b.getBookingId(), b.getUserId(), b.getFlightId(), b.getPnrCode(), b.getTripType(), b.getStatus(),
                b.getSeatIds(), b.getPassengerCount(), b.getBaseFare(), b.getTaxes(), b.getMealCost(),
                b.getBaggageCost(), b.getTotalFare(), b.getMealPreference(), b.getLuggageKg(),
                b.getContactEmail(), b.getContactPhone(), b.getPaymentId(), b.getBookedAt(), b.getUpdatedAt()
        );
    }
}
