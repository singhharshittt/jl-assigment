package com.jl.booking.service;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    AvailabilityService availabilityService;
    @Mock
    BookingRepository bookingRepository;
    @Mock
    VehicleRepository vehicleRepository;
    @Mock
    CleanerRepository cleanerRepository;

    @InjectMocks
    BookingService bookingService;

    // 1. Happy Path: createBooking success
    @Test
    void createBooking_success() {
        BookingRequestDto request = BookingRequestDto.builder()
                .date(LocalDate.of(2025, 10, 24))
                .startTime(LocalTime.of(10, 0))
                .durationHours(DurationType.TWO_HOURS)
                .cleanerCount(2)
                .build();

        VehicleAvailabilityDto vehicleAvailability = VehicleAvailabilityDto.builder().vehicleId(1L)
                .availableCleanerIds(List.of(1L, 2L, 3L)).requiredCleanerCount(3).build();
        Vehicle vehicle = Vehicle.builder().id(1L).name("Vehicle A").build();
        Cleaner c1 = Cleaner.builder().id(1L).name("C1").vehicle(vehicle).build();
        Cleaner c2 = Cleaner.builder().id(2L).name("C2").vehicle(vehicle).build();

        when(availabilityService.getAvailabilityByTimeSlot(any(), any(), any(), anyInt()))
                .thenReturn(List.of(vehicleAvailability));
        when(vehicleRepository.findByIdForBooking(1L)).thenReturn(Optional.of(vehicle));
        when(cleanerRepository.findByVehicleId(1L)).thenReturn(List.of(c1, c2));

        Booking booking = new Booking();
        booking.setId(99L);
        booking.setStartTime(LocalDateTime.of(2025, 10, 24, 10, 0));
        booking.setEndTime(LocalDateTime.of(2025, 10, 24, 12, 0));
        booking.setVehicle(vehicle);
        booking.setCleaners(List.of(c1, c2));

        when(bookingRepository.save(any())).thenReturn(booking);

        BookingResponseDto response = bookingService.createBooking(request);

        assertEquals(99L, response.bookingId());
        assertEquals(1L, response.vehicleId());
        assertEquals(List.of(1L, 2L), response.cleanerIds());
    }

    // 2. Failure: No available vehicles
    @Test
    void createBooking_noAvailableVehicles() {
        BookingRequestDto request = BookingRequestDto.builder()
                .date(LocalDate.of(2025, 10, 24))
                .startTime(LocalTime.of(10, 0))
                .durationHours(DurationType.TWO_HOURS)
                .cleanerCount(2)
                .build();

        when(availabilityService.getAvailabilityByTimeSlot(any(), any(), any(), anyInt()))
                .thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request));

        assertEquals("No available vehicle with sufficient cleaners", ex.getMessage());
    }

    // 3. Failure: Vehicle not found
    @Test
    void createBooking_vehicleNotFound() {
        BookingRequestDto request = BookingRequestDto.builder()
                .date(LocalDate.of(2025, 10, 24))
                .startTime(LocalTime.of(10, 0))
                .durationHours(DurationType.TWO_HOURS)
                .cleanerCount(2)
                .build();

        VehicleAvailabilityDto vehicleAvailability = VehicleAvailabilityDto.builder().vehicleId(99L)
                .availableCleanerIds(List.of(1L, 2L)).requiredCleanerCount(2).build();

        when(availabilityService.getAvailabilityByTimeSlot(any(), any(), any(), anyInt()))
                .thenReturn(List.of(vehicleAvailability));
        when(vehicleRepository.findByIdForBooking(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(request));

        assertEquals("Vehicle not found", ex.getMessage());
    }

    // 4. Cleaner ordering: lowest IDs selected
    @Test
    void createBooking_selectLowestIds() {
        BookingRequestDto request = BookingRequestDto.builder()
                .date(LocalDate.of(2025, 10, 24))
                .startTime(LocalTime.of(10, 0))
                .durationHours(DurationType.TWO_HOURS)
                .cleanerCount(2)
                .build();

        VehicleAvailabilityDto vehicleAvailability = VehicleAvailabilityDto.builder().vehicleId(1L)
                .availableCleanerIds(List.of(5L, 2L, 3L)).requiredCleanerCount(3).build();

        Vehicle vehicle = Vehicle.builder().id(1L).name("Vehicle A").build();
        Cleaner c1 = Cleaner.builder().id(5L).name("C5").vehicle(vehicle).build();
        Cleaner c2 = Cleaner.builder().id(2L).name("C2").vehicle(vehicle).build();
        Cleaner c3 = Cleaner.builder().id(3L).name("C3").vehicle(vehicle).build();

        when(availabilityService.getAvailabilityByTimeSlot(any(), any(), any(), anyInt()))
                .thenReturn(List.of(vehicleAvailability));
        when(vehicleRepository.findByIdForBooking(1L)).thenReturn(Optional.of(vehicle));
        when(cleanerRepository.findByVehicleId(1L)).thenReturn(List.of(c1, c2, c3));

        Booking booking = new Booking();
        booking.setId(100L);
        booking.setStartTime(LocalDateTime.of(2025, 10, 24, 10, 0));
        booking.setEndTime(LocalDateTime.of(2025, 10, 24, 12, 0));
        booking.setVehicle(vehicle);
        booking.setCleaners(List.of(c2, c3));

        when(bookingRepository.save(any())).thenReturn(booking);

        BookingResponseDto response = bookingService.createBooking(request);

        assertEquals(List.of(2L, 3L), response.cleanerIds());
    }

    // 1. Success: update with same cleaners available
    @Test
    void updateBooking_success_sameCleaners() {
        Vehicle vehicle = Vehicle.builder().id(1L).name("Vehicle A").build();
        Cleaner c1 = Cleaner.builder().id(1L).name("C1").vehicle(vehicle).build();
        Cleaner c2 = Cleaner.builder().id(2L).name("C2").vehicle(vehicle).build();

        Booking existing = Booking.builder()
                .id(10L)
                .status(BookingStatus.ACTIVE)
                .vehicle(vehicle)
                .cleaners(List.of(c1, c2)).cleanerCount(2)
                .durationMinutes(DurationType.TWO_HOURS.getMinutes())
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(existing));

        VehicleAvailabilityDto availability = VehicleAvailabilityDto.builder()
                .vehicleId(1L)
                .availableCleanerIds(List.of(1L, 2L))
                .requiredCleanerCount(2)
                .build();

        when(availabilityService.getAvailabilityByTimeSlot(any(), any(), any(), anyInt(), anyLong()))
                .thenReturn(List.of(availability));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingUpdateRequestDto request = BookingUpdateRequestDto.builder()
                .date(LocalDate.of(2025, 10, 25))
                .startTime(LocalTime.of(14, 0))
                .build();

        BookingResponseDto response = bookingService.updateBooking(10L, request);

        assertEquals(List.of(1L, 2L), response.cleanerIds());
        assertEquals(1L, response.vehicleId());
    }

    // 2. Success: update with different cleaners (same vehicle)
    @Test
    void updateBooking_success_differentCleaners() {
        Vehicle vehicle = Vehicle.builder().id(1L).name("Vehicle A").build();
        Cleaner c1 = Cleaner.builder().id(1L).name("C1").vehicle(vehicle).build();
        Cleaner c2 = Cleaner.builder().id(2L).name("C2").vehicle(vehicle).build();
        Cleaner c3 = Cleaner.builder().id(3L).name("C3").vehicle(vehicle).build();
        Cleaner c4 = Cleaner.builder().id(4L).name("C4").vehicle(vehicle).build();

        Booking existing = Booking.builder()
                .id(11L)
                .status(BookingStatus.ACTIVE)
                .vehicle(vehicle)
                .cleaners(List.of(c1, c2))
                .cleanerCount(2)
                .durationMinutes(DurationType.TWO_HOURS.getMinutes())
                .build();

        when(bookingRepository.findById(11L)).thenReturn(Optional.of(existing));

        VehicleAvailabilityDto availability = VehicleAvailabilityDto.builder()
                .vehicleId(1L)
                .availableCleanerIds(List.of(3L, 4L))
                .requiredCleanerCount(2)
                .build();

        when(availabilityService.getAvailabilityByTimeSlot(any(), any(), any(), anyInt(), anyLong()))
                .thenReturn(List.of(availability));
        when(cleanerRepository.findByVehicleId(1L)).thenReturn(List.of(c1, c2, c3, c4));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingUpdateRequestDto request = BookingUpdateRequestDto.builder()
                .date(LocalDate.of(2025, 10, 25))
                .startTime(LocalTime.of(15, 0))
                .build();

        BookingResponseDto response = bookingService.updateBooking(11L, request);

        assertEquals(List.of(3L, 4L), response.cleanerIds());
    }

    // 3. Failure: booking not found
    @Test
    void updateBooking_bookingNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        BookingUpdateRequestDto request = BookingUpdateRequestDto.builder()
                .date(LocalDate.of(2025, 10, 25))
                .startTime(LocalTime.of(14, 0))
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.updateBooking(99L, request));

        assertTrue(ex.getMessage().contains("Booking not found"));
    }

    // 4. Failure: cancelled booking
    @Test
    void updateBooking_cancelledBooking() {
        Vehicle vehicle = Vehicle.builder().id(1L).name("Vehicle A").build();

        Booking existing = Booking.builder()
                .id(12L)
                .status(BookingStatus.CANCELLED)
                .vehicle(vehicle)
                .build();

        when(bookingRepository.findById(12L)).thenReturn(Optional.of(existing));

        BookingUpdateRequestDto request = BookingUpdateRequestDto.builder()
                .date(LocalDate.of(2025, 10, 25))
                .startTime(LocalTime.of(14, 0))
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.updateBooking(12L, request));

        assertEquals("Cannot update cancelled booking", ex.getMessage());
    }

    // 5. Failure: no availability for original vehicle
    @Test
    void updateBooking_noAvailabilityForVehicle() {
        Vehicle vehicle = Vehicle.builder().id(1L).name("Vehicle A").build();
        Cleaner c1 = Cleaner.builder().id(1L).name("C1").vehicle(vehicle).build();

        Booking existing = Booking.builder()
                .id(13L)
                .status(BookingStatus.ACTIVE)
                .vehicle(vehicle)
                .cleaners(List.of(c1))
                .cleanerCount(2)
                .durationMinutes(DurationType.TWO_HOURS.getMinutes())
                .build();

        when(bookingRepository.findById(13L)).thenReturn(Optional.of(existing));

        VehicleAvailabilityDto otherVehicle = VehicleAvailabilityDto.builder()
                .vehicleId(2L)
                .availableCleanerIds(List.of(10L))
                .requiredCleanerCount(1)
                .build();

        when(availabilityService.getAvailabilityByTimeSlot(any(), any(), any(), anyInt(), anyLong()))
                .thenReturn(List.of(otherVehicle));

        BookingUpdateRequestDto request = BookingUpdateRequestDto.builder()
                .date(LocalDate.of(2025, 10, 25))
                .startTime(LocalTime.of(14, 0))
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bookingService.updateBooking(13L, request));

        assertEquals("No availability for original vehicle at new time", ex.getMessage());
    }

}
