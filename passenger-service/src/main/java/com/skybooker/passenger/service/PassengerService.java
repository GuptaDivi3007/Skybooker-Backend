package com.skybooker.passenger.service;

import com.skybooker.passenger.dto.*;

import java.util.List;

public interface PassengerService {

    PassengerResponse addPassenger(PassengerRequest request);

    List<PassengerResponse> addPassengers(PassengerBulkRequest request);

    PassengerResponse getPassengerById(String passengerId);

    List<PassengerResponse> getPassengersByBooking(String bookingId);

    PassengerResponse getByPassportNumber(String passportNumber);

    PassengerResponse getByTicketNumber(String ticketNumber);

    PassengerResponse updatePassenger(String passengerId, UpdatePassengerRequest request);

    PassengerResponse assignSeat(String passengerId, AssignSeatRequest request);

    PassengerResponse checkIn(String passengerId, CheckInRequest request);

    MessageResponse deletePassenger(String passengerId);

    MessageResponse deletePassengersByBooking(String bookingId);

    Long getPassengerCount(String bookingId);
}