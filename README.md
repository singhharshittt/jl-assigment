# 🚗 Vehicle Booking System

## 📖 Overview
The **Vehicle Booking System** is a Spring Boot–based backend service designed to manage cleaner scheduling and vehicle allocation with strict time-buffer constraints.

The system ensures:
- Efficient cleaner utilization
- Vehicle pickup/drop optimization
- 30-minute buffer enforcement between bookings
- Concurrency-safe booking operations

This project is implemented as part of an assignment with certain business assumptions due to incomplete scenario specifications.

---

## 🏗️ Architecture & Technology Stack

| Component            | Technology              |
|----------------------|-------------------------|
| Language             | Java 21                 |
| Framework            | Spring Boot             |
| Build Tool           | Gradle                  |
| ORM                  | Spring Data JPA         |
| Database             | H2 (In-Memory)          |
| API Documentation    | OpenAPI 3 / Swagger     |
| Concurrency Control  | Pessimistic Locking     |

---

## 🚀 Getting Started

### 1️⃣ Prerequisites
- Java 21 installed
- Gradle (or use wrapper)
- Git

### 2️⃣ Clone Repository
```bash
git clone https://github.com/singhharshittt/jl-assigment.git
```

### 3️⃣ Build the Project

Using Gradle wrapper:
```bash
./gradlew clean build
```
Skip tests if needed:
```bash
./gradlew clean build -x test
```

### 4️⃣ Run the Application

**Using Gradle**
```bash
./gradlew bootRun
```

## 🌐 Application Access

Default base URL:  
[http://localhost:8080](http://localhost:8080)

## 📘 API Documentation

This project uses **OpenAPI 3** for API specification and **Swagger UI** for interactive exploration.

### 🔹 Swagger UI
Interactive documentation available at:
- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

You can:
- Explore all REST endpoints
- Inspect request/response schemas
- Execute APIs directly from the browser

### 🔹 OpenAPI Specification (JSON)

The raw OpenAPI JSON specification is available at:  
[http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## 🗄️ Database Configuration (H2)

The application uses an **H2 in-memory database** for development and demonstration purposes.

### 🔹 H2 Console
Access the H2 console at:  
[http://localhost:8080/h2-console](http://localhost:8080/h2-console)

**Default Connection Settings**

| Property   | Value              |
|------------|--------------------|
| JDBC URL   | jdbc:h2:mem:testdb |
| Username   | sa                 |
| Password   | (empty)            |

### 🔹 Important Notes About H2
- Runs fully in memory
- Data is lost when the application restarts
- Intended for development/testing only
- Replace with MySQL/PostgreSQL for production environments

## 📌 Core Business Rules & Design Decisions

Due to limited assignment details, the following assumptions are applied.

### 1 Time & Scheduling Rules
- Working Hours: **08:00 – 22:00 (UAE timezone, UTC+4)**
- First appointment: Start ≥ 08:00
- Last appointment: End ≤ 22:00
- Max Start Times:
  - 18:00 (for 4-hour bookings)
  - 20:00 (for 2-hour bookings)

**30-Minute Break Rule**  
`end1 + 30 minutes ≤ start2`

Example:
- 10:00–12:00 → Next booking 12:30 ✅
- 10:00–12:00 → Next booking 12:29 ❌

**Non-Working Days**
- Gregorian Fridays → Vehicles and cleaners unavailable

---

### 2 Vehicle Capacity Constraint (Critical)
**Design Principle**  
Vehicles act as shared transportation resources, not full-time slot holders.

They are only responsible for:
- Cleaner pickup
- Cleaner drop-off

To maximize cleaner utilization:
- A strict 30-minute buffer is enforced between pickup/drop operations.
- Assumption: All pickup/drop operations can be completed within 30 minutes.

**Example Scheduling : Consider one vehicle with 5 cleaners**
- 10-12 : C1,C2 ✓
- 10:30-12:30 : C3,C4 ✓
- 11-13 : C5 ✓
- 13:30-3:30 : C1, C2, C3 ✓

---

### 3 Availability API Contract
- `GET /api/availability?date=2025-10-24`  
  → Per-cleaner free slots (max continuous periods ≥ 2hr)

- `GET /api/availability?date=2025-10-24&startTime=14:00&durationHours=2&cleanerCount=2`  
  → Per-vehicle available cleaner sets

---
### 4. Booking Creation Logic
**Request:** `{date, startTime, durationHours, cleanerCount}`

**Auto-select strategy:**
1. Find first vehicle with ≥ `cleanerCount` free cleaners
2. Assign lowest-ID free cleaners from that vehicle
3. Fail if no vehicle satisfies both constraints

> No client-specified cleaner IDs (server auto-selects for simplicity)

---

### 5. Booking Update Rules
- `PUT /api/bookings/{id}`
- **Update logic:**
  - Try same cleaners at new time → ✅ Success
  - Try different cleaners from same vehicle → ✅ Success
  - Fail if update requires changing vehicle → ❌
- Cannot change **duration** or **cleanerCount** on update

---

### 6. Data Validation
- **Duration:** Exactly 120 or 240 minutes
- **Date format:** `yyyy-MM-dd` (API), stored as `LocalDateTime (UTC)`
- **Cleaner count:** 1, 2, or 3
- No customer/location data (per spec simplicity)

---

### 7. Concurrency Control
- `@Transactional` with **PESSIMISTIC_WRITE lock** on Vehicle during:
  - Availability check (for target time window)
  - Booking creation/update
- Prevents race conditions (e.g., 2 bookings grabbing same vehicle cleaners)

---

### 8. Availability Slot Generation
For each cleaner on given date:
1. Start with `[08:00, 22:00]`
2. Subtract all **ACTIVE bookings** with 30min buffers
3. Split into max continuous free periods
4. Filter slots ≥ requested duration

---