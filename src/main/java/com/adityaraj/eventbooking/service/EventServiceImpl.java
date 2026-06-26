package com.adityaraj.eventbooking.service;

import com.adityaraj.eventbooking.model.Event;
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

    @Override
    public void deleteById(Long id) {
        eventRepository.deleteById(id);
    }
}
