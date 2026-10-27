# Event Booking System

A backend-focused web application built with Spring Boot that lets users browse events, book seats and manage bookings, while admins manage events and view all bookings. Built to learn and apply core Spring Boot concepts: MVC architecture, Spring Data JPA, Spring Security, and server-side rendering with Thymeleaf.

**Live demo:** [eventbooking-8dbw.onrender.com](https://eventbooking-8dbw.onrender.com) (Render free tier, so the first load can take 30-60 seconds while the server wakes up)

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Architecture](#project-architecture)
- [Entity Relationships](#entity-relationships)
- [Application Flow](#application-flow)
- [Concurrency Handling](#concurrency-handling)
- [Security](#security)
- [Deployment](#deployment)
- [How to Run Locally](#how-to-run-locally)
- [Demo Credentials](#demo-credentials)
- [Project Structure](#project-structure)
- [Known Limitations and Future Enhancements](#known-limitations-and-future-enhancements)

---

## Overview

This project simulates a real-world event booking platform (similar in concept to BookMyShow) with two roles: **Admin** and **User**. Admins create, edit and delete events and view all bookings. Users browse events, book seats, go through a payment confirmation step, and manage their own bookings.

The focus is on the backend: layered architecture (Controller → Service → Repository → Database), role-based security, validation, centralized error handling, and safe handling of concurrent bookings.

---

## Features

### Admin
- Create, edit and delete events
- View all bookings made by all users
- Cannot delete an event that has active bookings
- Admin-only routes are protected and hidden from regular users

### User
- Register and log in securely (passwords are BCrypt-hashed)
- Browse events with live seat availability
- Search events by name or venue
- Book seats (number of people is configurable)
- Payment confirmation step before the booking is finalized (simulated)
- View and cancel own bookings; cancelling returns the seats to the event

### System-level
- **Role-based access control** enforced in the UI (Thymeleaf + Spring Security extras) and in the backend (Spring Security filter chain)
- **Concurrency-safe booking** using `@Transactional` for atomicity and `@Version` (optimistic locking) to prevent overbooking
- **Centralized error handling** with custom exceptions (e.g. not enough seats, deleting an event with bookings) shown as friendly error pages
- **Form validation** using Jakarta Bean Validation (required fields, minimum 1 person, valid email, minimum password length)
- **Search** using Spring Data JPA query derivation (no manual SQL)

---

## Tech Stack

| Layer           | Technology                                                                      |
| --------------- | ------------------------------------------------------------------------------- |
| Language        | Java 21                                                                         |
| Framework       | Spring Boot 3                                                                   |
| Web Layer       | Spring MVC                                                                      |
| Data Access     | Spring Data JPA + Hibernate                                                     |
| Security        | Spring Security (form login, role-based authorization, BCrypt password hashing) |
| Database        | MySQL                                                                           |
| Templating      | Thymeleaf (server-side rendering)                                               |
| UI              | Bootstrap 5 (via CDN)                                                           |
| Build Tool      | Maven                                                                           |
| Validation      | Jakarta Bean Validation (Hibernate Validator)                                   |
| Containerization| Docker (multi-stage build)                                                      |
| Deployment      | Render                                                                          |

> This is intentionally a backend-heavy project. Thymeleaf is a minimal templating layer to demonstrate the full request-response flow without a separate frontend.

---

## Project Architecture

Standard **layered MVC architecture**:

```
Browser (Thymeleaf-rendered HTML)
        |
        v
   Controller          <-- handles HTTP requests, talks to Service only
        |
        v
   Service (interface) <-- business logic, loosely coupled via interfaces
        |
        v
ServiceImpl (class)    <-- implementation, injected via Spring DI
        |
        v
   Repository           <-- extends JpaRepository
        |
        v
   MySQL Database
```

- **Controller** never talks to the Repository directly; it goes through the Service layer, keeping HTTP concerns separate from business logic.
- **Service is an interface** (`EventService`, `BookingService`) with a separate implementation class, so the Controller depends on the abstraction (loose coupling).
- **Repositories extend `JpaRepository<Entity, ID>`**, giving `save()`, `findAll()`, `findById()`, `deleteById()` out of the box. Query methods such as `findByEventNameContainingOrVenueContaining` are derived from method names.
- **Dependency Injection** (`@Autowired`) wires Services, Repositories and Controllers through Spring's IoC container.

---

## Entity Relationships

```
User ----------------< Booking >---------------- Event
(one user can have      (many-to-one on        (one event can have
 many bookings)          both sides)             many bookings)
```

- **User**: id, name, email, password (hashed), mobileNo, role (ADMIN/USER), dob, gender
- **Event**: id, eventName, eventDescription, venue, eventType, date, startTime, endTime, price, totalCapacity, availableCapacity, lastBookingDate, **version** (used for optimistic locking)
- **Booking**: id, bookingDate, bookingTime, numberOfPeople, bookingStatus (PENDING/CONFIRMED), and `@ManyToOne` relations to `User` and `Event` via `user_id` and `event_id`

Tables are generated by Hibernate from these entities (`spring.jpa.hibernate.ddl-auto=update`).

---

## Application Flow

### Booking flow (core feature)

1. User opens `/events` and clicks **Book Now**.
2. `GET /booking/event/{eventId}` shows the event details (read-only) and an editable "number of people" field.
3. On submit, `POST /booking/event/{eventId}`:
   - Validates input (`@Valid` + `BindingResult`)
   - Checks `availableCapacity >= numberOfPeople`, otherwise throws `NotEnoughSeatsException`
   - Decrements the event's `availableCapacity` and saves the booking with status `PENDING`, both inside one `@Transactional` method
   - Redirects to `/payment/{bookingId}`
4. The payment page shows the total (`numberOfPeople x price`) and a **Pay Now** button (simulated payment, no real gateway).
5. `POST /payment/{bookingId}/confirm` changes the booking status to `CONFIRMED`.
6. From `/my-bookings`, a user can cancel a booking; this adds the seats back to the event and deletes the booking.

Seats are reserved when the booking is created (PENDING), so nobody else can take them while the user is on the payment page.

### Search flow
`GET /events/search?keyword=...` passes the keyword to `findByEventNameContainingOrVenueContaining(keyword, keyword)`; Spring Data JPA derives a `LIKE '%keyword%'` query on name and venue.

### Event deletion safety
Before deleting an event, the service checks for bookings (`findByEventId`). If any exist, deletion is blocked with a clear error message.

---

## Concurrency Handling

Two users booking the last seat at the same time must not both succeed. Two mechanisms work together:

| Mechanism | Role |
|---|---|
| `@Transactional` | **Atomicity.** The seat update and the booking insert are committed together or rolled back together. |
| `@Version` on `Event` | **Conflict detection (optimistic locking).** Prevents a stale concurrent update from overwriting another one. |

**How `@Version` prevents overbooking:**

1. Two users read the same event (e.g. `availableCapacity = 1`, `version = 5`).
2. Both send an update. Hibernate adds the version check to the SQL:
   `UPDATE event SET available_capacity=?, version=6 WHERE id=? AND version=5`
3. The database locks the row during the update, so updates run one at a time. The first one succeeds and bumps the version to 6.
4. The second update matches **0 rows** (version is no longer 5), so Hibernate throws `OptimisticLockingFailureException`. That transaction rolls back and the user sees a "please try again" message.

Exactly one booking succeeds. The same protection applies to cancellations, which also update the event's capacity.

---

## Security

Implemented with **Spring Security 6**:

- **Authentication:** custom `UserDetailsService` (`CustomUserDetailsService`) loads users by email; `DaoAuthenticationProvider` uses it with a `BCryptPasswordEncoder`.
- **Authorization** (in `SecurityConfig` via `SecurityFilterChain`):
  - `/events`, `/login`, `/register`, `/` are public
  - `/admin/**`, `/users` require `ROLE_ADMIN`
  - everything else requires authentication
- **CSRF protection** is enabled; every state-changing form includes a CSRF token.
- **UI-level enforcement:** `thymeleaf-extras-springsecurity6` (`sec:authorize`) shows or hides links and buttons by role, while the backend still blocks direct access to restricted routes.
- **GET vs POST:** every action that changes data (cancel booking, confirm payment, delete event) uses `POST`.

---

## Deployment

The app is **containerized with Docker** and deployed on **Render**.

**Dockerfile (multi-stage build):**
1. **Build stage:** a Maven + Java 21 image copies the project and builds the jar (`mvn clean install -DskipTests`).
2. **Run stage:** a lightweight Java 21 JRE image copies only the built jar and runs it (`java -jar app.jar`), exposing port 8080.

The multi-stage approach keeps the final image small because it contains only the jar and the JRE, not Maven or the source code.

**Configuration:** the database connection is supplied through environment variables, not hardcoded:

```
SPRING_DATASOURCE_URL=jdbc:mysql://<host>:<port>/<database>
SPRING_DATASOURCE_USERNAME=<username>
SPRING_DATASOURCE_PASSWORD=<password>
```

`spring.jpa.hibernate.ddl-auto=update` lets Hibernate create the tables on first boot, so no manual schema setup is needed.

**Deployment history:** the app was first deployed on Railway and later moved to Render after the Railway free credits ran out.

**Challenges faced:**

1. **Java version mismatch.** The first build failed with `error: release version 25 not supported`. `pom.xml` targeted Java 25 only because that was the JDK installed locally. Fixed by changing `<java.version>` to 21, a widely supported LTS release.
2. **Database connection failure.** The app crashed with a communications link failure because the database URL resolved with an empty host and port. The environment variables were referenced incorrectly. Fixed by correcting the variable names and values.
3. **Empty production database.** The hosted database was separate from the local one, so no events, users or admin account existed. Resolved by inserting sample events, registering an account through `/register`, and promoting it to `ADMIN` with a manual `UPDATE` query. This showed why seed data and migrations matter.

---

## How to Run Locally

### Prerequisites
- Java 17+ (project targets 21)
- Maven
- MySQL Server running locally

### Steps

1. Clone the repository
   ```
   git clone https://github.com/Adityaraj1005/eventbooking.git
   cd eventbooking
   ```
2. Create a MySQL database
   ```
   CREATE DATABASE eventbooking;
   ```
3. Update `src/main/resources/application.properties`
   ```
   spring.datasource.url=jdbc:mysql://localhost:3306/eventbooking
   spring.datasource.username=root
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   ```
4. Run the application
   ```
   ./mvnw spring-boot:run
   ```
5. Open `http://localhost:8080`. Tables are created automatically on first run.
6. Register a user (default role `USER`), or set a user's `role` to `ADMIN` in MySQL to test admin features.

### Run with Docker
```
docker build -t eventbooking .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://<host>:3306/eventbooking \
  -e SPRING_DATASOURCE_USERNAME=<username> \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  eventbooking
```

---

## Demo Credentials

| Role  | Email            | Password  |
| ----- | ---------------- | --------- |
| Admin | admin@test.com   | admin1234 |
| User  | Register via `/register` | n/a |

The admin account can create, edit and delete events and view all bookings. A regular user can browse events, book seats and manage their own bookings.

---

## Project Structure

```
src/main/java/com/adityaraj/eventbooking/
├── model/          Event, User, Booking entities (JPA annotated)
├── repository/     EventRepository, UserRepository, BookingRepository
├── service/        Service interfaces + ServiceImpl classes (business logic)
├── controller/     EventController, UserController, BookingController, LoginController
├── config/         SecurityConfig (Spring Security setup)
├── exception/      Custom exceptions + GlobalExceptionHandler (@ControllerAdvice)
└── EventbookingApplication.java

src/main/resources/
├── templates/      Thymeleaf pages (events, booking, payment, my-bookings, admin pages, navbar fragment)
└── application.properties

Dockerfile          Multi-stage build (Maven build, then JRE runtime)
```

---

## Known Limitations and Future Enhancements

**Known limitations**
- A `PENDING` booking holds its seats indefinitely if the user never pays. Planned fix: a scheduled job that cancels PENDING bookings older than ~10 minutes and returns the seats.
- No idempotency token, so a double-submitted booking form can create two bookings for the same user.
- Under very heavy load on a single event, optimistic locking makes many users retry; pessimistic locking (`SELECT ... FOR UPDATE`) or a queue would suit that case better.

**Planned enhancements**
- Real payment gateway (Razorpay/Stripe) instead of simulated payment
- Email notifications on booking confirmation and cancellation
- Admin ability to cancel or manage any user's booking
- Dedicated event detail page before booking
- REST APIs to support a separate frontend (React/Angular)

---

## Author

**Adityaraj Shyamsundar Bhandari**
[GitHub](https://github.com/Adityaraj1005) · [LinkedIn](https://www.linkedin.com/in/adityaraj-shyamsundar-bhandari-135358324/)
