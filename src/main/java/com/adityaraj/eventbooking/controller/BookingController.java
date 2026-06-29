package com.adityaraj.eventbooking.controller;

import com.adityaraj.eventbooking.model.Booking;
import com.adityaraj.eventbooking.model.Event;
import com.adityaraj.eventbooking.model.User;
import com.adityaraj.eventbooking.service.BookingService;
import com.adityaraj.eventbooking.service.EventService;
import com.adityaraj.eventbooking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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


    //Principal represents the currently logged in user!
    //When someone logs in → Spring Security stores their
    // information → Principal gives you access to it!
    //
    //What it contains:
    //principal.getName() → returns the username of logged in user
    //In your app username = email address!
    //So:
    //javaprincipal.getName() → "admin@test.com"
    @PostMapping("/booking/event/{eventId}")
    public String booking(@PathVariable Long eventId,
                          @Valid @ModelAttribute Booking booking,
                          BindingResult result,
                          Principal principal,
                          Model model) {
        if(result.hasErrors()) {
            Event event = eventService.findById(eventId);
            model.addAttribute("event", event);
            return "booking";
            //When going back to booking form — page needs event
            // details to display again (event name, venue, price)!
            //Without this → booking page shows empty — no event details!
        }
        Event event = eventService.findById(eventId);
        User user = userService.findByEmail(principal.getName());
        booking.setEvent(event);
        booking.setUser(user);
        booking.setBookingDate(LocalDate.now());
        booking.setBookingStatus("PENDING");
        bookingService.saveBooking(booking);
        return "redirect:/payment/" + booking.getId();
    }

    @GetMapping("/my-bookings")
    public String showBookingById(Model model,Principal principal) {
        User user = userService.findByEmail(principal.getName());
        List<Booking> bookings = bookingService.getBookingByUserId(user.getId());
        model.addAttribute("bookings", bookings);
        return "my-bookings";
    }

    //Viewing something → @GetMapping
    //Changing something → @PostMapping
    @PostMapping("/booking/cancel/{id}")
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

    @GetMapping("/payment/{bookingId}")
    public String showPaymentPage(@PathVariable Long bookingId, Model model) {
        Booking booking = bookingService.getBookingById(bookingId);
        model.addAttribute("booking", booking);
        return "payment";
    }


    //addFlashAttribute → stores message temporarily → survives redirect → available on next page!
    @PostMapping("/payment/{bookingId}/confirm")
    public String confirmPayment(@PathVariable Long bookingId,
                                 RedirectAttributes redirectAttributes) {
        bookingService.confirmBooking(bookingId);
        redirectAttributes.addFlashAttribute("successMessage", "Booking confirmed successfully! 🎉");
        return "redirect:/my-bookings";
    }
}
