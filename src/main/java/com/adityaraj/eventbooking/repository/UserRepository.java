package com.adityaraj.eventbooking.repository;

import com.adityaraj.eventbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);// writes queries find by email automatically
}
