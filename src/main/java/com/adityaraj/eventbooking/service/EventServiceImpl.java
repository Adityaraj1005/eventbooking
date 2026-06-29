package com.adityaraj.eventbooking.service;

import com.adityaraj.eventbooking.model.Booking;
import com.adityaraj.eventbooking.model.Event;
import com.adityaraj.eventbooking.repository.BookingRepository;
import com.adityaraj.eventbooking.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    @Autowired
    EventRepository eventRepository;
    @Override
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @Override
    public void saveEvent(Event event) {
        eventRepository.save(event);
    }

    @Override
    public Event findById(Long id) {
        return eventRepository.findById(id).orElseThrow();
    }

    @Autowired
    BookingRepository bookingRepository;

    @Override
    public void deleteById(Long id) {
        List<Booking> bookings = bookingRepository.findByEventId(id);
        if(!bookings.isEmpty()) {
            throw new RuntimeException("Cannot delete event with existing bookings! Cancel all bookings first.");
        }
        eventRepository.deleteById(id);
    }
    @Override
    public List<Event> searchEvents(String keyword) {
        return eventRepository.findByEventNameContainingOrVenueContaining(keyword, keyword);
    }
}
