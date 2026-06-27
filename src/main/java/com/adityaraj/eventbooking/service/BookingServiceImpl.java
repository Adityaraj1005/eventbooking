package com.adityaraj.eventbooking.service;

import com.adityaraj.eventbooking.model.Booking;
import com.adityaraj.eventbooking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {


    @Autowired
    BookingRepository bookingRepository;

    @Override
    public void saveBooking(Booking booking) {
        bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getBookingByUserId(long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public void cancelBooking(long bookingId) {
        bookingRepository.deleteById(bookingId);
    }
}
