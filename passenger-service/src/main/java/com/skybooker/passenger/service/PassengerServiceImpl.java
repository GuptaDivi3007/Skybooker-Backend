package com.skybooker.passenger.service;

import com.skybooker.passenger.dto.*;
import com.skybooker.passenger.entity.PassengerInfo;
import com.skybooker.passenger.entity.PassengerType;
import com.skybooker.passenger.exception.ResourceNotFoundException;
import com.skybooker.passenger.repository.PassengerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class PassengerServiceImpl implements PassengerService {

    private final PassengerRepository passengerRepository;

    public PassengerServiceImpl(PassengerRepository passengerRepository) {
        this.passengerRepository = passengerRepository;
    }

    @Override
    public PassengerResponse addPassenger(PassengerRequest request) {
        validatePassengerData(request.dateOfBirth(), request.passportExpiry());

        PassengerInfo passenger = new PassengerInfo();
        passenger.setBookingId(normalizeRequired(request.bookingId(), "Booking id"));
        passenger.setTitle(normalizeRequired(request.title(), "Title"));
        passenger.setFirstName(normalizeRequired(request.firstName(), "First name"));
        passenger.setLastName(normalizeRequired(request.lastName(), "Last name"));
        passenger.setDateOfBirth(request.dateOfBirth());
        passenger.setGender(request.gender());
        passenger.setPassportNumber(normalizeRequired(request.passportNumber(), "Passport number").toUpperCase());
        passenger.setNationality(normalizeRequired(request.nationality(), "Nationality"));
        passenger.setPassportExpiry(request.passportExpiry());
        passenger.setPassengerType(request.passengerType() == null ? detectPassengerType(request.dateOfBirth()) : request.passengerType());
        passenger.setTicketNumber(generateTicketNumber());
        passenger.setCheckedIn(false);

        PassengerInfo saved = passengerRepository.save(passenger);
        return mapToResponse(saved);
    }

    @Override
    public List<PassengerResponse> addPassengers(PassengerBulkRequest request) {
        if (request.passengers() == null || request.passengers().isEmpty()) {
            throw new IllegalArgumentException("Passenger list cannot be empty");
        }

        return request.passengers()
                .stream()
                .map(this::addPassenger)
                .toList();
    }

    @Override
    public PassengerResponse getPassengerById(String passengerId) {
        return mapToResponse(findPassenger(passengerId));
    }

    @Override
    public List<PassengerResponse> getPassengersByBooking(String bookingId) {
        return passengerRepository.findByBookingId(bookingId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PassengerResponse getByPassportNumber(String passportNumber) {
        PassengerInfo passenger = passengerRepository.findByPassportNumberIgnoreCase(passportNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with passport number: " + passportNumber));

        return mapToResponse(passenger);
    }

    @Override
    public PassengerResponse getByTicketNumber(String ticketNumber) {
        PassengerInfo passenger = passengerRepository.findByTicketNumberIgnoreCase(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with ticket number: " + ticketNumber));

        return mapToResponse(passenger);
    }

    @Override
    public PassengerResponse updatePassenger(String passengerId, UpdatePassengerRequest request) {
        PassengerInfo passenger = findPassenger(passengerId);

        if (request.title() != null && !request.title().isBlank()) {
            passenger.setTitle(request.title().trim());
        }

        if (request.firstName() != null && !request.firstName().isBlank()) {
            passenger.setFirstName(request.firstName().trim());
        }

        if (request.lastName() != null && !request.lastName().isBlank()) {
            passenger.setLastName(request.lastName().trim());
        }

        if (request.dateOfBirth() != null) {
            passenger.setDateOfBirth(request.dateOfBirth());
        }

        if (request.gender() != null) {
            passenger.setGender(request.gender());
        }

        if (request.passportNumber() != null && !request.passportNumber().isBlank()) {
            passenger.setPassportNumber(request.passportNumber().trim().toUpperCase());
        }

        if (request.nationality() != null && !request.nationality().isBlank()) {
            passenger.setNationality(request.nationality().trim());
        }

        if (request.passportExpiry() != null) {
            passenger.setPassportExpiry(request.passportExpiry());
        }

        validatePassengerData(passenger.getDateOfBirth(), passenger.getPassportExpiry());

        if (request.passengerType() != null) {
            passenger.setPassengerType(request.passengerType());
        } else {
            passenger.setPassengerType(detectPassengerType(passenger.getDateOfBirth()));
        }

        PassengerInfo saved = passengerRepository.save(passenger);
        return mapToResponse(saved);
    }

    @Override
    public PassengerResponse assignSeat(String passengerId, AssignSeatRequest request) {
        PassengerInfo passenger = findPassenger(passengerId);

        passenger.setSeatId(normalizeRequired(request.seatId(), "Seat id"));
        passenger.setSeatNumber(normalizeRequired(request.seatNumber(), "Seat number").toUpperCase());

        PassengerInfo saved = passengerRepository.save(passenger);
        return mapToResponse(saved);
    }

    @Override
    public PassengerResponse checkIn(String passengerId, CheckInRequest request) {
        PassengerInfo passenger = findPassenger(passengerId);

        if (passenger.isCheckedIn()) {
            throw new IllegalArgumentException("Passenger is already checked in");
        }

        passenger.setSeatId(normalizeRequired(request.seatId(), "Seat id"));
        passenger.setSeatNumber(normalizeRequired(request.seatNumber(), "Seat number").toUpperCase());
        passenger.setCheckedIn(true);
        passenger.setCheckInTime(LocalDateTime.now());

        PassengerInfo saved = passengerRepository.save(passenger);
        return mapToResponse(saved);
    }

    @Override
    public MessageResponse deletePassenger(String passengerId) {
        PassengerInfo passenger = findPassenger(passengerId);
        passengerRepository.delete(passenger);

        return new MessageResponse("Passenger deleted successfully");
    }

    @Override
    public MessageResponse deletePassengersByBooking(String bookingId) {
        Long count = passengerRepository.countByBookingId(bookingId);
        passengerRepository.deleteByBookingId(bookingId);

        return new MessageResponse("Deleted " + count + " passengers for booking " + bookingId);
    }

    @Override
    public Long getPassengerCount(String bookingId) {
        return passengerRepository.countByBookingId(bookingId);
    }

    private PassengerInfo findPassenger(String passengerId) {
        return passengerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passenger not found with id: " + passengerId));
    }

    private void validatePassengerData(LocalDate dateOfBirth, LocalDate passportExpiry) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }

        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }

        if (passportExpiry == null) {
            throw new IllegalArgumentException("Passport expiry is required");
        }

        if (!passportExpiry.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Passport must not be expired");
        }
    }

    private PassengerType detectPassengerType(LocalDate dateOfBirth) {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();

        if (age < 2) {
            return PassengerType.INFANT;
        }

        if (age < 12) {
            return PassengerType.CHILD;
        }

        return PassengerType.ADULT;
    }

    private String generateTicketNumber() {
        String ticket;

        do {
            ticket = "TKT" + (1000000000L + Math.abs(new Random().nextLong() % 9000000000L));
        } while (passengerRepository.existsByTicketNumber(ticket));

        return ticket;
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim();
    }

    private PassengerResponse mapToResponse(PassengerInfo passenger) {
        return new PassengerResponse(
                passenger.getPassengerId(),
                passenger.getBookingId(),
                passenger.getTitle(),
                passenger.getFirstName(),
                passenger.getLastName(),
                passenger.getDateOfBirth(),
                passenger.getGender(),
                passenger.getPassportNumber(),
                passenger.getNationality(),
                passenger.getPassportExpiry(),
                passenger.getSeatId(),
                passenger.getSeatNumber(),
                passenger.getTicketNumber(),
                passenger.getPassengerType(),
                passenger.isCheckedIn(),
                passenger.getCheckInTime(),
                passenger.getCreatedAt(),
                passenger.getUpdatedAt()
        );
    }
}