package com.adityaraj.eventbooking.service;

import com.adityaraj.eventbooking.model.Event;


import java.util.List;

public interface EventService {

    List<Event> getAllEvents();

    void saveEvent(Event event);

    Event findById(Long id);

    void deleteById(Long id);

    List<Event> searchEvents(String keyword);
}
