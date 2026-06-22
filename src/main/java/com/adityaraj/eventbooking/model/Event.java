package com.adityaraj.eventbooking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data // generates getter setter to_string() for all fields
@NoArgsConstructor// needed to create objects when fetching from database
@AllArgsConstructor
@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String eventName;
    String eventDescription;
    String venue;
    String eventType;
    LocalDate date;
    LocalTime startTime;
    LocalTime endTime;
    Double price;
    Integer totalCapacity;
    Integer availableCapacity;
    LocalDate lastBookingDate;
}
