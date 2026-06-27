package com.adityaraj.eventbooking.controller;

import com.adityaraj.eventbooking.model.Booking;
import com.adityaraj.eventbooking.model.Event;
import com.adityaraj.eventbooking.model.User;
import com.adityaraj.eventbooking.service.BookingService;
import com.adityaraj.eventbooking.service.EventService;
import com.adityaraj.eventbooking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class BookingController {

    @Autowired
    BookingService bookingService;

    @Autowired
    EventService eventService;

    @Autowired
    UserService userService;

    @GetMapping("/booking/event/{eventId}")
    public String showBookingForEvent(@PathVariable Long eventId, Model model) {
        Event event = eventService.findById(eventId);
        model.addAttribute("event", event);
        model.addAttribute("booking", new Booking());
        return "booking";
    }

    @PostMapping("/booking/event/{eventId}")
    public String booking(@PathVariable Long eventId, @ModelAttribute Booking booking, Principal principal) {
        Event event = eventService.findById(eventId);
        User user = userService.findByEmail(principal.getName());
        booking.setEvent(event);
        booking.setUser(user);
        booking.setBookingDate(LocalDate.now());
        booking.setBookingStatus("CONFIRMED");
        bookingService.saveBooking(booking);


        return "redirect:/my-bookings";
    }

    @GetMapping("/my-bookings")
    public String showBookingById(Model model,Principal principal) {
        User user = userService.findByEmail(principal.getName());
        List<Booking> bookings = bookingService.getBookingByUserId(user.getId());
        model.addAttribute("bookings", bookings);
        return "my-bookings";
    }

    @GetMapping("/booking/cancel/{id}")
    public String cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return "redirect:/my-bookings";
    }

    @GetMapping("/admin/bookings")
    public String showAllBookings(Model model) {
        List<Booking> bookings = bookingService.getAllBookings();
        model.addAttribute("bookings", bookings);
        return "admin-bookings";
    }
}
