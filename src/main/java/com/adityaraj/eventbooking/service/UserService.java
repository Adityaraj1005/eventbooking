package com.adityaraj.eventbooking.service;

import com.adityaraj.eventbooking.model.User;

import java.util.List;

public interface UserService {


    List<User> findAll();

    void save(User user);

    User findById(Long id);

    void deleteById(Long id);

    User findByEmail(String email);
}
