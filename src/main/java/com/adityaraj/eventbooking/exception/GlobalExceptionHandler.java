package com.adityaraj.eventbooking.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


//@ExceptionHandler(NotEnoughSeatsException.class) → catches specifically NotEnoughSeatsException
//Puts error message in Model
//Returns error page
//GlobalExceptionHandler is like a safety net —
// when anything goes wrong anywhere in your app, it
// catches it and shows a friendly page instead of crashing!
//With GlobalExceptionHandler:
//Write error handling ONCE → works for ALL controllers automatically!
//@ControllerAdvice = "watch ALL controllers and handle exceptions globally"
@ControllerAdvice//looks all controller for exception
public class GlobalExceptionHandler {

    @ExceptionHandler(NotEnoughSeatsException.class)
    public String handleNotEnoughSeats(NotEnoughSeatsException ex, Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
}