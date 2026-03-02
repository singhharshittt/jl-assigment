package com.jl.booking.service;

import com.jl.booking.exception.ValidationException;
import com.jl.booking.model.dto.CleanerAvailabilityDto;
import com.jl.booking.model.dto.VehicleAvailabilityDto;
import com.jl.booking.model.entity.Booking;
import com.jl.booking.model.entity.Cleaner;
import com.jl.booking.model.entity.Vehicle;
import com.jl.booking.model.enums.DurationType;
import com.jl.booking.model.util.TimeSlot;
import com.jl.booking.repository.BookingRepository;
import com.jl.booking.repository.CleanerRepository;
import com.jl.booking.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {

    protected static final LocalTime WORKING_START = LocalTime.of(8, 0);
    protected static final LocalTime WORKING_END = LocalTime.of(22, 0);
    private static final Duration BREAK_DURATION = Duration.ofMinutes(30);

    private final BookingRepository bookingRepository;
    private final CleanerRepository cleanerRepository;
    private final VehicleRepository vehicleRepository;

    public List<CleanerAvailabilityDto> getAvailabilityByDate(LocalDate date) {
        log.info("Checking availability by date: {}", date);
        validateWorkingDay(date);

        return cleanerRepository.findAll().stream()
                .map(cleaner -> new CleanerAvailabilityDto(cleaner,
                        calculateCleanerFreeSlots(cleaner, date)))
                .filter(dto -> !dto.freeSlots().isEmpty())
                .peek(dto -> log.debug("Cleaner {} free slots: {}", dto.cleanerId(), dto.freeSlots()))
                .toList();
    }

    public List<VehicleAvailabilityDto> getAvailabilityByTimeSlot(LocalDate date,
                                                                  LocalTime startTime,
                                                                  DurationType duration,
                                                                  int cleanerCount) {
        return getAvailabilityByTimeSlot(date, startTime, duration, cleanerCount, null);
    }

    public List<VehicleAvailabilityDto> getAvailabilityByTimeSlot(LocalDate date,
                                                                  LocalTime startTime,
                                                                  DurationType duration,
                                                                  int cleanerCount,
                                                                  Long excludeBookingId) {
        log.info("Checking availability by time slot: date={}, startTime={}, duration={}, cleanerCount={}, excludeBookingId={}",
                date, startTime, duration, cleanerCount, excludeBookingId);

        validateWorkingDay(date);

        LocalDateTime start = date.atTime(startTime);
        LocalDateTime end = start.plusMinutes(duration.getMinutes());
        validateTimeWindow(start, end);

        return vehicleRepository.findAllWithCleaners().stream()
                .filter(vehicle -> isVehicleFree(vehicle, date, start, end, excludeBookingId))
                .map(vehicle -> buildVehicleAvailability(vehicle, start, end, cleanerCount, excludeBookingId))
                .filter(Objects::nonNull)
                .toList();
    }

    private VehicleAvailabilityDto buildVehicleAvailability(Vehicle vehicle,
                                                            LocalDateTime start,
                                                            LocalDateTime end,
                                                            int cleanerCount,
                                                            Long excludeBookingId) {
        List<Cleaner> availableCleaners = getAvailableCleaners(vehicle, start, end, excludeBookingId);
        if (availableCleaners.size() < cleanerCount) {
            return null;
        }
        return new VehicleAvailabilityDto(vehicle, availableCleaners, cleanerCount);
    }

    protected boolean isVehicleFree(Vehicle vehicle, LocalDate date,
                                    LocalDateTime start, LocalDateTime end, Long excludeBookingId) {

        List<Booking> allBookingsForDay =
                bookingRepository.findByVehicleAndDate(vehicle, date);

        if (excludeBookingId != null) {
            allBookingsForDay = excludeBooking(allBookingsForDay, excludeBookingId);
        }

        if (allBookingsForDay.isEmpty()) {
            return true;
        }

        long bufferMinutes = BREAK_DURATION.toMinutes();

        for (Booking booking : allBookingsForDay) {

            LocalDateTime existingStart = booking.getStartTime();
            LocalDateTime existingEnd = booking.getEndTime();

            // Check new start against existing start & end
            if (isWithinBuffer(start, existingStart, bufferMinutes) ||
                    isWithinBuffer(start, existingEnd, bufferMinutes) ||

                    // Check new end against existing start & end
                    isWithinBuffer(end, existingStart, bufferMinutes) ||
                    isWithinBuffer(end, existingEnd, bufferMinutes)) {

                return false;
            }
        }

        return true;
    }

    private boolean isWithinBuffer(LocalDateTime t1,
                                   LocalDateTime t2,
                                   long bufferMinutes) {

        return Math.abs(Duration.between(t1, t2).toMinutes()) < bufferMinutes;
    }

    private List<Cleaner> getAvailableCleaners(Vehicle vehicle, LocalDateTime start, LocalDateTime end, Long excludeBookingId) {
        return vehicle.getCleaners().stream()
                .filter(cleaner -> isCleanerFree(cleaner, start, end, excludeBookingId))
                .toList();
    }

    private boolean isCleanerFree(Cleaner cleaner, LocalDateTime testStart, LocalDateTime testEnd, Long excludeBookingId) {
        List<Booking> overlapping = bookingRepository.findByCleanerIdAndDate(cleaner.getId(), testStart.toLocalDate());
        if (excludeBookingId != null) {
            overlapping = excludeBooking(overlapping, excludeBookingId);
        }
        return overlapping.stream().noneMatch(b ->
                testStart.isBefore(b.getEndTime().plus(BREAK_DURATION)) &&
                        testEnd.isAfter(b.getStartTime().minus(BREAK_DURATION)));
    }

    private List<Booking> excludeBooking(List<Booking> bookings, Long excludeBookingId) {
        return bookings.stream()
                .filter(b -> !b.getId().equals(excludeBookingId))
                .toList();
    }

    private List<TimeSlot> calculateCleanerFreeSlots(Cleaner cleaner, LocalDate date) {
        LocalDateTime dayStart = date.atTime(WORKING_START);
        LocalDateTime dayEnd = date.atTime(WORKING_END);

        List<TimeSlot> bookedSlots = bookingRepository.findByCleanerIdAndDate(cleaner.getId(), date).stream()
                .map(b -> new TimeSlot(b.getStartTime(), b.getEndTime()))
                .sorted(Comparator.comparing(TimeSlot::start))
                .toList();

        List<TimeSlot> freeSlots = new ArrayList<>();
        LocalDateTime cursor = dayStart;

        for (TimeSlot booked : bookedSlots) {
            LocalDateTime bufferedStart = booked.start().minus(BREAK_DURATION);
            if (cursor.isBefore(bufferedStart)) {
                freeSlots.add(new TimeSlot(cursor, bufferedStart));
            }
            cursor = booked.end().plus(BREAK_DURATION);
        }

        if (cursor.isBefore(dayEnd)) {
            freeSlots.add(new TimeSlot(cursor, dayEnd));
        }

        return freeSlots.stream()
                .filter(slot -> Duration.between(slot.start(), slot.end()).toMinutes() >= 120)
                .collect(Collectors.groupingBy(TimeSlot::start,
                        Collectors.minBy(Comparator.comparing(TimeSlot::end))))
                .values().stream()
                .flatMap(Optional::stream)
                .toList();
    }

    private void validateWorkingDay(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.FRIDAY) {
            throw new ValidationException("No bookings on Fridays");
        }
    }

    private void validateTimeWindow(LocalDateTime start, LocalDateTime end) {
        LocalDateTime dayStart = LocalDateTime.of(start.toLocalDate(), WORKING_START);
        LocalDateTime dayEnd = LocalDateTime.of(start.toLocalDate(), WORKING_END);

        if (start.isBefore(dayStart) || end.isAfter(dayEnd)) {
            throw new ValidationException("Booking outside working hours 08:00-22:00");
        }
    }
}
