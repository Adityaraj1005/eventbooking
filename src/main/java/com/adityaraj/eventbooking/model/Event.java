package com.adityaraj.eventbooking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "Event name is required")
    String eventName;

    @NotBlank(message = "Description is required")
    String eventDescription;

    @NotBlank(message = "Venue is required")
    String venue;

    @NotBlank(message = "Event type is required")
    String eventType;

    @NotNull(message = "Date is required")
    LocalDate date;

    @NotNull(message = "Start time is required")
    LocalTime startTime;

    @NotNull(message = "End time is required")
    LocalTime endTime;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    Double price;

    @NotNull(message = "Total capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    Integer totalCapacity;

    @NotNull(message = "Available capacity is required")
    Integer availableCapacity;

    @NotNull(message = "Last booking date is required")
    LocalDate lastBookingDate;
}
