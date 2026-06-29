package com.adityaraj.eventbooking.controller;

import com.adityaraj.eventbooking.model.Event;
import com.adityaraj.eventbooking.service.EventService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class EventController {

    @Autowired
    EventService eventService;

    @GetMapping("/events")
    public String showEvents(Model model) {
        List<Event> events = eventService.getAllEvents();
        model.addAttribute("events", events);//adding to model naming them as events
        return "events";
    }


    //just shows the form
    @GetMapping("/admin/events/add")
    public String showAddEventForm(Model model) {
        model.addAttribute("event", new Event());
        return "add-event";
    }


    // //Added @Valid before @ModelAttribute → triggers validation
    //    //Added BindingResult result → holds all validation errors
    //    //Added if(result.hasErrors()) → if any field fails → go back to register page with errors
    @PostMapping("/admin/events/add")
    public String addEvents(@Valid @ModelAttribute Event event,
                            BindingResult result) {
        if(result.hasErrors()) {
            return "add-event";
        }
        eventService.saveEvent(event);
        return "redirect:/events";
    }

    @GetMapping("/admin/events/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Event event = eventService.findById(id);
        model.addAttribute("event", event);
        return "edit-event";
    }

    @PostMapping("/admin/events/edit/{id}")
    public String editEvents(@ModelAttribute Event event) {
        eventService.saveEvent(event);
        return "redirect:/events";
    }

    @GetMapping("/admin/events/delete/{id}")
    public String deleteEvent(@PathVariable Long id) {
        eventService.deleteById(id);
        return "redirect:/events";
    }


    //@RequestParam extracts the search term from URL.
    //URL looks like: /events/search?keyword=Pune
    //@RequestParam takes keyword=Pune and puts "Pune" into keyword variable!
    @GetMapping("/events/search")
    public String searchEvents(@RequestParam String keyword, Model model) {
        List<Event> events = eventService.searchEvents(keyword);
        model.addAttribute("events", events);
        return "events";
    }
}
