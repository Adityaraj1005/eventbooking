package com.adityaraj.eventbooking.service;

import com.adityaraj.eventbooking.exception.NotEnoughSeatsException;
import com.adityaraj.eventbooking.model.Booking;
import com.adityaraj.eventbooking.model.Event;
import com.adityaraj.eventbooking.repository.BookingRepository;
import com.adityaraj.eventbooking.repository.EventRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {


    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    EventRepository eventRepository;


    @Transactional //"I used @Transactional annotation to handle concurrent booking requests,
    // ensuring data consistency when multiple users book simultaneously."
    @Override
    public void saveBooking(Booking booking) {
       
        try {
            Event event = booking.getEvent();
            if(event.getAvailableCapacity() >= booking.getNumberOfPeople()) {
                event.setAvailableCapacity(event.getAvailableCapacity() - booking.getNumberOfPeople());
                eventRepository.save(event);
                bookingRepository.save(booking);
            } else {
                throw new NotEnoughSeatsException("Not enough seats available!");
            }
        } catch (OptimisticLockingFailureException e) {
            throw new NotEnoughSeatsException("Seat availability just changed, please try booking again!");
        }
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
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        Event event = booking.getEvent();
        event.setAvailableCapacity(event.getAvailableCapacity() + booking.getNumberOfPeople());
        eventRepository.save(event);
        bookingRepository.deleteById(bookingId);
    }

    @Override
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElseThrow();
    }

    @Override
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setBookingStatus("CONFIRMED");
        bookingRepository.save(booking);
    }
}
