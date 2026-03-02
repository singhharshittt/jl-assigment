# 🏗️ Design Decisions - Justlife Booking System

*Assignment document didn't contain end to end scenarios and details. So we will be going ahead with below assumptions*

---

## Table of Contents
1. [Time & Scheduling Rules](#1-time--scheduling-rules)
2. [Vehicle Capacity Constraint (Critical)](#2-vehicle-capacity-constraint-critical)
3. [Availability API Contract](#3-availability-api-contract)
4. [Booking Creation Logic](#4-booking-creation-logic)
5. [Booking Update Rules](#5-booking-update-rules)
6. [Data Validation](#6-data-validation)
7. [Concurrency Control](#7-concurrency-control)
8. [Availability Slot Generation](#8-availability-slot-generation)

---

## 1. Time & Scheduling Rules
- **Working Hours:** 08:00–22:00 (UAE timezone, UTC+4)
- **First appointment:** start ≥ 08:00
- **Last appointment:** end ≤ 22:00 
- **Max start times:**
    - 18:00 (for 4hr bookings)
    - 20:00 (for 2hr bookings)
- **30-Min Break Rule:** `end1 + 30min ≤ start2` (inclusive)
    - Example: `10:00–12:00 → Next: 12:30 ✓, 12:29 ❌`
- **Non-Working Days:** Gregorian Fridays (vehicle + cleaner unavailable)

---

## 2. Vehicle Capacity Constraint (Critical)
- **Vehicle = Shared Transportation Resource**
- Vehicle will just be responsible for pickups and drops and won't be idle for entire slot.
- We will just ensure 30 mins buffer between each pick up/drop.
- This will ensure we can maximize cleaners scheduling.
- Assumption : All pickup/drops can be reached withing 30 mins.


- **Example** :
- 10-12 : C1,C2 ✓
- 10:30-12:30 : C3,C4 ✓
- 11-13 : C5 ✓
- 13:30-3:30 : C1, C2, C3 ✓

---

## 3. Availability API Contract
- `GET /api/availability?date=2025-10-24`  
  → Per-cleaner free slots (max continuous periods ≥ 2hr)

- `GET /api/availability?date=2025-10-24&startTime=14:00&durationHours=2&cleanerCount=2`  
  → Per-vehicle available cleaner sets

---

## 4. Booking Creation Logic
**Request:** `{date, startTime, durationHours, cleanerCount}`

**Auto-select strategy:**
1. Find first vehicle with ≥ `cleanerCount` free cleaners
2. Assign lowest-ID free cleaners from that vehicle
3. Fail if no vehicle satisfies both constraints

> No client-specified cleaner IDs (server auto-selects for simplicity)

---

## 5. Booking Update Rules
- `PUT /api/bookings/{id}`
- **Update logic:**
    - Try same cleaners at new time → ✅ Success
    - Try different cleaners from same vehicle → ✅ Success
    - Fail if update requires changing vehicle → ❌
- Cannot change **duration** or **cleanerCount** on update

---

## 6. Data Validation
- **Duration:** Exactly 120 or 240 minutes
- **Date format:** `yyyy-MM-dd` (API), stored as `LocalDateTime (UTC)`
- **Cleaner count:** 1, 2, or 3
- No customer/location data (per spec simplicity)

---

## 7. Concurrency Control
- `@Transactional` with **PESSIMISTIC_WRITE lock** on Vehicle during:
    - Availability check (for target time window)
    - Booking creation/update
- Prevents race conditions (e.g., 2 bookings grabbing same vehicle cleaners)

---

## 8. Availability Slot Generation
For each cleaner on given date:
1. Start with `[08:00, 22:00]`
2. Subtract all **ACTIVE bookings** with 30min buffers
3. Split into max continuous free periods
4. Filter slots ≥ requested duration

---