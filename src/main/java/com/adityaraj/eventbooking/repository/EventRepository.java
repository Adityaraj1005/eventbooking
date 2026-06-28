package com.adityaraj.eventbooking.repository;

import com.adityaraj.eventbooking.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


//By extending JpaRepository we automatically get:
//save(),findAll(),findById(),deleteById(),and 10+ more methods
//
//All without writing a single line of implementation!

//"JpaRepository is an interface provided by Spring Data JPA.
// By extending it, Spring automatically creates the implementation at runtime with all basic CRUD methods.
// I don't need to write any SQL or implementation code for basic operations."

public interface EventRepository  extends JpaRepository<Event, Long> {
    // JpaRepository<Event, Long>
// First parameter 'Event' → which entity this repository manages
// Second parameter 'Long' → data type of primary key (id field)

    List<Event> findByEventNameContainingOrVenueContaining(String name, String venue);
    //Breaking down the method name:
    //findBy → SELECT * FROM event WHERE
    //EventName → event_name column
    //Containing → LIKE '%keyword%'
    //Or → OR
    //Venue → venue column
    //Containing → LIKE '%keyword%'
}
