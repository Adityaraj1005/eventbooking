package com.adityaraj.eventbooking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data // generates getter setter to_string() for all fields
@NoArgsConstructor// needed to create objects when fetching from database
@AllArgsConstructor
@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    LocalDate bookingDate;
    Integer numberOfPeople;
    String bookingStatus;
    LocalTime bookingTime;

    @ManyToOne//one user many bookings
    @JoinColumn(name = "user_id")//creates user_id column in booking table as foreign key pointing to user table
    User user;

    @ManyToOne//one event many bookings
    @JoinColumn(name = "event_id")
    Event event;


}
