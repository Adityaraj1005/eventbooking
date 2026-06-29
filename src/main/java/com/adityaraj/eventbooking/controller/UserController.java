package com.adityaraj.eventbooking.controller;

import com.adityaraj.eventbooking.model.Event;
import com.adityaraj.eventbooking.model.User;
import com.adityaraj.eventbooking.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class UserController {

    @Autowired
    UserService userService;

    @GetMapping("/users")
    public String showUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "users";
    }

    //just shows the form
    @GetMapping("/admin/users/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        return "add-user";
    }

    @PostMapping("/admin/users/add")//@ModelAttribute automatically fetches all data for user
    public String addUsers(@ModelAttribute User user) {
        userService.save(user);
        return "redirect:/users";//avoids duplicate submission
    }


    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }


    //Added @Valid before @ModelAttribute → triggers validation
    //Added BindingResult result → holds all validation errors
    //Added if(result.hasErrors()) → if any field fails → go back to register page with errors
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute User user,
                               BindingResult result) {
        if(result.hasErrors()) {
            return "register";
        }
        user.setRole("USER");
        userService.save(user);
        return "redirect:/login";
    }
}
