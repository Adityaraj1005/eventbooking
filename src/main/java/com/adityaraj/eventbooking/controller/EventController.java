package com.adityaraj.eventbooking.controller;

import com.adityaraj.eventbooking.model.Event;
import com.adityaraj.eventbooking.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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

    @PostMapping("/admin/events/add")//@ModelAttribute automatically fetches all data for event
    public String addEvents(@ModelAttribute Event event) {
        eventService.saveEvent(event);
        return "redirect:/events";//avoids duplicate submission
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
}
