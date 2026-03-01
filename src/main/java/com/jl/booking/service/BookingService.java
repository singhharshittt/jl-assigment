package com.jl.booking.service;

import com.jl.booking.exception.ConflictException;
import com.jl.booking.exception.NotFoundException;
import com.jl.booking.exception.ValidationException;
import com.jl.booking.model.dto.BookingRequestDto;
import com.jl.booking.model.dto.BookingResponseDto;
import com.jl.booking.model.dto.BookingUpdateRequestDto;
import com.jl.booking.model.dto.VehicleAvailabilityDto;
import com.jl.booking.model.entity.Booking;
import com.jl.booking.model.entity.Cleaner;
import com.jl.booking.model.entity.Vehicle;
import com.jl.booking.model.enums.BookingStatus;
import com.jl.booking.model.enums.DurationType;
import com.jl.booking.repository.BookingRepository;
import com.jl.booking.repository.CleanerRepository;
import com.jl.booking.repository.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final AvailabilityService availabilityService;
    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final CleanerRepository cleanerRepository;

    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request) {
        log.info("Creating booking request: {}", request);

        LocalDateTime start = request.date().atTime(request.startTime());
        LocalDateTime end = start.plusMinutes(request.durationHours().getMinutes());

        List<VehicleAvailabilityDto> availableVehicles = availabilityService.getAvailabilityByTimeSlot(
                request.date(), request.startTime(), request.durationHours(), request.cleanerCount());

        if (availableVehicles.isEmpty()) {
            log.warn("No available vehicles found for request: {}", request);
            throw new ConflictException("No available vehicle with sufficient cleaners");
        }

        VehicleAvailabilityDto vehicleAvailability = availableVehicles.getFirst();
        Vehicle vehicle = vehicleRepository.findByIdForBooking(vehicleAvailability.vehicleId())
                .orElseThrow(() -> {
                    log.error("Vehicle not found: id={}", vehicleAvailability.vehicleId());
                    return new NotFoundException("Vehicle not found");
                });

        List<Cleaner> selectedCleaners = selectCleaners(vehicle, vehicleAvailability.availableCleanerIds(), request.cleanerCount());
        log.debug("Selected cleaners for booking: {}", selectedCleaners.stream().map(Cleaner::getId).toList());

        Booking booking = buildBooking(start, end, request, vehicle, selectedCleaners);
        Booking saved = bookingRepository.save(booking);

        log.info("Booking created successfully: id={}, vehicleId={}, cleaners={}",
                saved.getId(), vehicle.getId(), selectedCleaners.stream().map(Cleaner::getId).toList());

        return buildResponse(saved, vehicle, selectedCleaners);
    }

    @Transactional
    public BookingResponseDto updateBooking(Long bookingId, BookingUpdateRequestDto request) {
        log.info("Updating booking: id={}, newDate={}, newStartTime={}", bookingId, request.date(), request.startTime());

        Booking existingBooking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.error("Booking not found: id={}", bookingId);
                    return new NotFoundException("Booking not found: " + bookingId);
                });

        if (existingBooking.getStatus() != BookingStatus.ACTIVE) {
            log.warn("Attempt to update cancelled booking: id={}", bookingId);
            throw new ValidationException("Cannot update cancelled booking");
        }

        Vehicle originalVehicle = existingBooking.getVehicle();
        DurationType originalDuration = DurationType.valueOfMinutes(existingBooking.getDurationMinutes());

        LocalDateTime newStart = request.date().atTime(request.startTime());
        LocalDateTime newEnd = newStart.plusMinutes(existingBooking.getDurationMinutes());
        log.debug("New booking times: start={}, end={}", newStart, newEnd);

        VehicleAvailabilityDto availableVehicle = availabilityService
                .getAvailabilityByTimeSlot(request.date(), request.startTime(), originalDuration,
                        existingBooking.getCleanerCount(), existingBooking.getId())
                .stream()
                .filter(vehicle -> originalVehicle.getId().equals(vehicle.vehicleId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("No availability for original vehicle at new time: vehicleId={}, bookingId={}",
                            originalVehicle.getId(), bookingId);
                    return new ConflictException("No availability for original vehicle at new time");
                });

        List<Cleaner> finalCleaners = resolveCleaners(existingBooking.getCleaners(), originalVehicle, availableVehicle);
        log.debug("Final cleaners selected for update: {}", finalCleaners.stream().map(Cleaner::getId).toList());

        existingBooking.setStartTime(newStart);
        existingBooking.setEndTime(newEnd);
        existingBooking.setCleaners(finalCleaners);

        Booking updated = bookingRepository.save(existingBooking);
        log.info("Booking updated successfully: id={}, vehicleId={}, cleaners={}",
                updated.getId(), originalVehicle.getId(), finalCleaners.stream().map(Cleaner::getId).toList());

        return buildResponse(updated, originalVehicle, finalCleaners);
    }

    // ----------------- Private Helpers -----------------

    private List<Cleaner> selectCleaners(Vehicle vehicle, List<Long> availableCleanerIds, int cleanerCount) {
        return cleanerRepository.findByVehicleId(vehicle.getId()).stream()
                .filter(c -> availableCleanerIds.contains(c.getId()))
                .sorted(Comparator.comparingLong(Cleaner::getId))
                .limit(cleanerCount)
                .toList();
    }

    private List<Cleaner> resolveCleaners(List<Cleaner> originalCleaners, Vehicle vehicle, VehicleAvailabilityDto availability) {
        boolean allOriginalAvailable = new HashSet<>(availability.availableCleanerIds())
                .containsAll(originalCleaners.stream().map(Cleaner::getId).toList());

        if (allOriginalAvailable) {
            return originalCleaners;
        }

        return cleanerRepository.findByVehicleId(vehicle.getId()).stream()
                .filter(c -> availability.availableCleanerIds().contains(c.getId()))
                .sorted(Comparator.comparing(Cleaner::getId))
                .limit(originalCleaners.size())
                .toList();
    }

    private Booking buildBooking(LocalDateTime start, LocalDateTime end,
                                 BookingRequestDto request, Vehicle vehicle, List<Cleaner> cleaners) {
        Booking booking = new Booking();
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setDurationMinutes(request.durationHours().getMinutes());
        booking.setCleanerCount(request.cleanerCount());
        booking.setVehicle(vehicle);
        booking.setCleaners(cleaners);
        return booking;
    }

    private BookingResponseDto buildResponse(Booking booking, Vehicle vehicle, List<Cleaner> cleaners) {
        return new BookingResponseDto(
                booking.getId(),
                booking.getStartTime().toString(),
                booking.getEndTime().toString(),
                vehicle.getId(),
                cleaners.stream().map(Cleaner::getId).toList()
        );
    }
}

