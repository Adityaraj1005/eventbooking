# Event Booking System

A backend-focused web application built with Spring Boot that allows users to browse events, book seats, and manage bookings, while admins manage events and view all bookings. Built from scratch to understand and apply core Spring Boot concepts — MVC architecture, Spring Data JPA, Spring Security, and server-side rendering with Thymeleaf.

**Live demo:** [eventbooking-production-291b.up.railway.app](https://eventbooking-production-291b.up.railway.app)
Deployed on Railway, running against a live MySQL instance (also hosted on Railway).

---

## Table of Contents
- [Overview](#overview)
- [Live Demo](#live-demo)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Architecture](#project-architecture)
- [Entity Relationships](#entity-relationships)
- [Application Flow](#application-flow)
- [Security](#security)
- [Deployment](#deployment)
- [How to Run Locally](#how-to-run-locally)
- [Demo Credentials](#demo-credentials)
- [Project Structure](#project-structure)
- [Future Enhancements](#future-enhancements)

---

## Overview

This project simulates a real-world event booking platform (similar in concept to BookMyShow) with two types of users — **Admin** and **User**. Admins manage events (create, edit, delete) and view all bookings. Users browse events, book seats, go through a payment confirmation flow, and manage their own bookings.

The project was built as a learning exercise to deeply understand Spring Boot's layered architecture, and every layer (Controller → Service → Repository → Database) was implemented manually without using starter templates or generated CRUD code, so that each line of code is understood end-to-end.

---

## Live Demo

The application is deployed on [Railway](https://railway.app) and is publicly accessible:

**[https://eventbooking-production-291b.up.railway.app](https://eventbooking-production-291b.up.railway.app)**

The Spring Boot app and its MySQL database both run as separate Railway services within the same project, connected via Railway-managed environment variables (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) referencing the database service's auto-generated credentials. `application.properties` reads these via Spring's `${ENV_VAR:fallback}` syntax, so the exact same codebase works unmodified both locally (falling back to local MySQL credentials) and in production (picking up Railway's injected values). See [Deployment](#deployment) for details.

Use the [demo credentials](#demo-credentials) below to log in as an admin or register a new account as a regular user.

---

## Features

### Admin
- Create, edit, and delete events
- View all bookings made by all users
- Cannot delete an event that has active bookings
- Restricted access — admin-only routes are protected and hidden from regular users

### User
- Register and log in securely (passwords are hashed, never stored in plain text)
- Browse all events with live seat availability
- Search/filter events by name or venue
- Book seats for an event (number of people is configurable)
- Go through a payment confirmation step before booking is finalized
- View and cancel their own bookings
- Cancelling a booking automatically returns the seats to the event's available pool

### System-level
- **Role-based access control** — Admin and User see different navigation options and have different permissions, enforced both in the UI (Thymeleaf + Spring Security extras) and at the backend (Spring Security filter chain)
- **Concurrency-safe booking** — `@Transactional` ensures that two users booking the last available seat simultaneously cannot both succeed (prevents overbooking)
- **Centralized error handling** — custom exceptions (e.g., not enough seats, deleting an event with bookings) are caught globally and shown as friendly error pages instead of stack traces
- **Form validation** — both required-field and business-rule validation (e.g., minimum 1 person per booking, valid email format, minimum password length) handled on the backend using Jakarta Bean Validation
- **Search** — implemented using Spring Data JPA query derivation (no manual SQL)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3 |
| Web Layer | Spring MVC |
| Data Access | Spring Data JPA + Hibernate |
| Security | Spring Security (form login, role-based authorization, BCrypt password hashing) |
| Database | MySQL |
| Templating | Thymeleaf (server-side rendering) |
| UI | Bootstrap 5 (via CDN) |
| Build Tool | Maven |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Version Control | Git & GitHub |

> Note: This is intentionally a backend-heavy project. Thymeleaf is used only as a minimal templating layer to demonstrate the full request-response data flow without building a separate frontend. All business logic, security, and data access live in Java.

---

## Project Architecture

The application follows a standard **layered MVC architecture**:

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
ServiceImpl (class)    <-- actual implementation, injected via Spring DI
        |
        v
   Repository           <-- extends JpaRepository, no manual SQL needed
        |
        v
   MySQL Database
```

**Why this layering matters:**
- **Controller** never talks to the Repository directly — it always goes through the Service layer. This keeps HTTP concerns (requests, redirects, view names) separate from business logic.
- **Service is an interface** (e.g., `EventService`, `BookingService`) implemented by a separate class (e.g., `EventServiceImpl`). This is **loose coupling** — the Controller depends on the interface, not the concrete implementation, so the implementation could be swapped without touching the Controller.
- **Repository interfaces extend `JpaRepository<Entity, ID>`**, which gives `save()`, `findAll()`, `findById()`, `deleteById()` automatically. Spring Data JPA also generates queries from method names alone (e.g., `findByEventNameContainingOrVenueContaining`) — no SQL is written by hand anywhere in this project.
- **Dependency Injection** (`@Autowired`) is used throughout — Spring creates and wires all the Service, Repository, and Controller objects automatically via its IoC container, instead of the code creating objects with `new`.

---

## Entity Relationships

Four core entities, connected through JPA relationships:

```
User ----------------< Booking >---------------- Event
(one user can have      (many-to-one on        (one event can have
 many bookings)          both sides)             many bookings)
```

- **User**: id, name, email, password (hashed), mobileNo, role (ADMIN/USER), dob, gender
- **Event**: id, eventName, eventDescription, venue, eventType, date, startTime, endTime, price, totalCapacity, availableCapacity, lastBookingDate
- **Booking**: id, bookingDate, bookingTime, numberOfPeople, bookingStatus (PENDING/CONFIRMED), and `@ManyToOne` relationships to both `User` and `Event` via `user_id` and `event_id` foreign keys

Tables are **auto-generated by Hibernate** from these entity classes (`spring.jpa.hibernate.ddl-auto=update`) — no manual schema/DDL was written.

---

## Application Flow

### Booking flow (the core feature)
1. User browses `/events` and clicks **Book Now** on an event
2. `GET /booking/event/{eventId}` shows a form pre-filled with event details (read-only) and an editable "number of people" field
3. On submit, `POST /booking/event/{eventId}`:
   - Validates the booking (`@Valid` + `BindingResult`) — rejects 0 or empty values
   - Checks `event.getAvailableCapacity() >= numberOfPeople` — throws a custom `NotEnoughSeatsException` if not enough seats, caught globally and shown as a friendly error page
   - Saves the booking with status `PENDING` and decrements `availableCapacity` on the event — both writes happen inside a single `@Transactional` method so they either both succeed or both roll back, and so that concurrent booking requests can't both read the same "before" capacity and overbook the event
   - Redirects to `/payment/{bookingId}`
4. Payment page shows a calculated total (`numberOfPeople x price`) and a **Pay Now** button (simulated payment — no real gateway integrated, see [Future Enhancements](#future-enhancements))
5. On confirming payment, `POST /payment/{bookingId}/confirm` updates the booking status to `CONFIRMED` and redirects to `/my-bookings` with a flash success message
6. From `/my-bookings`, a user can cancel any of their own bookings — this **adds the seats back** to the event's `availableCapacity` and deletes the booking record

### Search flow
- A keyword typed into the search box on `/events` triggers `GET /events/search?keyword=...`
- `@RequestParam` extracts the keyword, which is passed to `EventRepository.findByEventNameContainingOrVenueContaining(keyword, keyword)` — Spring Data JPA derives a `LIKE '%keyword%'` query on both the event name and venue columns from the method name alone

### Event deletion safety
- Before deleting an event, the service checks if any bookings reference it (`findByEventId`)
- If bookings exist, deletion is blocked with a clear error message instead of breaking the foreign key relationship in the database

---

## Security

Implemented with **Spring Security 6**:

- **Authentication**: Custom `UserDetailsService` (`CustomUserDetailsService`) loads users from the database by email and wraps them in Spring Security's `UserDetails` object. A `DaoAuthenticationProvider` is configured with this service plus a `BCryptPasswordEncoder` so passwords are never compared or stored in plain text.
- **Authorization**: Defined declaratively in `SecurityConfig` using `SecurityFilterChain`:
  - `/events`, `/login`, `/register`, `/` — public
  - `/admin/**`, `/users` — restricted to `ROLE_ADMIN`
  - everything else — requires authentication
- **CSRF protection** is enabled by default — every state-changing form (login, register, add/edit/delete event, booking, cancellation, payment) includes a hidden CSRF token field
- **UI-level enforcement**: `thymeleaf-extras-springsecurity6` is used with `sec:authorize="hasRole('ADMIN')"` / `sec:authorize="isAuthenticated()"` / `sec:authorize="isAnonymous()"` to show/hide navigation links and Edit/Delete buttons based on the logged-in user's role — so a regular user never even sees an Edit/Delete button, in addition to the backend blocking the route if they tried to hit it directly
- **GET vs POST discipline**: every action that *changes* data (cancel booking, confirm payment, delete event) uses `POST`, not `GET`, both for correctness and for CSRF protection. Read-only navigation uses `GET`.

---

## Deployment

Deployed on **Railway**, which builds directly from this GitHub repository on every push to `master`.

**Setup:**
1. The Spring Boot app and a MySQL database are provisioned as two separate services inside one Railway project, so they share a private network and can reference each other's environment variables.
2. Railway auto-detects the Maven project and runs `./mvnw clean install` to build it, then starts the resulting jar — no Dockerfile required.
3. The database connection is wired up by setting three environment variables on the app service, referencing the MySQL service's auto-generated credentials using Railway's variable-reference syntax:
   ```
   SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}
   SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
   SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
   ```
4. `spring.jpa.hibernate.ddl-auto=update` means Hibernate creates all tables automatically against the fresh Railway database on first boot — no manual schema migration was needed.
5. A public domain was generated for the app service to expose it to the internet (`*.up.railway.app`).

**Notable fix during deployment:** the project's `pom.xml` originally targeted Java 25 (set locally to match the only JDK available in the dev environment at the time). Railway's build image didn't support that version, so it was changed to Java 21 (a standard LTS release) — this is a good example of why pinning to a widely-supported LTS version matters for portability across environments.


**Challenges faced during deployment:**

1. **Build failure on Java version.** First deployment attempt failed at the compile step with `error: release version 25 not supported`. The local `pom.xml` had been set to Java 25 earlier purely because that was the only JDK installed locally at the time — Railway's build image didn't recognize that version at all. Fixed by changing `<java.version>` to 21.

2.Issue: App crashed due to malformed database URL
After deployment, the app crashed with a Communications link failure error. The database URL was resolving to jdbc:mysql://: with empty host and port.
Cause: I referenced MySQL environment variables without the service name prefix (${{MYSQLHOST}} instead of ${{MySQL.MYSQLHOST}}). Railway requires the full service name when referencing variables from a different service in the same project.
Fix: Updated the database URL to:
jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}

3. **Production database started empty.** Since the Railway MySQL instance is a completely separate database from the local one used during development, none of the locally-created events, users, or admin account existed in production. Resolved by inserting sample event data directly via Railway's built-in database query interface, registering a fresh admin account through the live `/register` page, and promoting it to `ADMIN` with a manual `UPDATE` query — a good practical illustration of why seed data and database migrations matter for real deployments.

---

## How to Run Locally

The app is live at the link above, so this section is only needed if you want to run the actual source code yourself (e.g. to inspect it in an IDE, modify it, or run it offline).

### Prerequisites
- Java 17+ (project currently targets 21)
- Maven
- MySQL Server running locally

### Steps
1. Clone the repository
   ```
   git clone https://github.com/Adityaraj1005/eventbooking.git
   cd eventbooking
   ```

2. Create a MySQL database
   ```sql
   CREATE DATABASE eventbooking;
   ```

3. Update `src/main/resources/application.properties` with your MySQL credentials
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/eventbooking
   spring.datasource.username=root
   spring.datasource.password=your_password
   spring.jpa.hibernate.ddl-auto=update
   ```

4. Run the application
   ```
   ./mvnw spring-boot:run
   ```

5. Open `http://localhost:8080` in your browser. Tables are created automatically on first run.

6. Register a new user (defaults to role `USER`), or manually update a user's `role` column to `ADMIN` in MySQL to test admin features.

---

## Demo Credentials

Try these directly on the [live demo](#live-demo):

| Role | Email | Password |
|---|---|---|
| Admin | admin@test.com | admin1234 |
| User | Register your own via the `/register` page | — |

The admin account can create/edit/delete events and view all bookings across all users. A regular user can browse events, book seats, and manage their own bookings.

---

## Project Structure

```
src/main/java/com/adityaraj/eventbooking/
├── model/          Event, User, Booking entities (JPA annotated)
├── repository/     EventRepository, UserRepository, BookingRepository (JpaRepository)
├── service/        Service interfaces + ServiceImpl classes (business logic)
├── controller/      EventController, UserController, BookingController, LoginController
├── config/         SecurityConfig (Spring Security setup)
├── exception/       Custom exceptions + GlobalExceptionHandler (@ControllerAdvice)
└── EventbookingApplication.java

src/main/resources/
├── templates/        Thymeleaf HTML pages (events, booking, payment, my-bookings, admin pages, navbar fragment, etc.)
└── application.properties
```

---

## Future Enhancements

Features planned for later, not yet implemented:

- Integrate a real payment gateway (Razorpay/Stripe) in place of the simulated payment confirmation
- Email notifications on booking confirmation/cancellation
- Admin ability to cancel/manage any user's booking from the admin dashboard
- Dedicated event detail page before booking
- Convert MVC endpoints to REST APIs to support a separate frontend (React/Angular) in place of Thymeleaf
- Dockerize the application for a more portable, reproducible deployment setup

## Author

**Adityaraj Shyamsundar Bhandari**
[GitHub](https://github.com/Adityaraj1005) · [LinkedIn](https://www.linkedin.com/in/adityaraj-shyamsundar-bhandari-135358324/)
