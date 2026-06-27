package com.adityaraj.eventbooking.service;

import com.adityaraj.eventbooking.model.Booking;

import java.util.List;

public interface BookingService {

    void saveBooking(Booking booking);
    List<Booking> getBookingByUserId(long userId);

    List<Booking> getAllBookings();

    void cancelBooking(long bookingId);

    Booking getBookingById(Long id);
    void confirmBooking(Long bookingId);
}
