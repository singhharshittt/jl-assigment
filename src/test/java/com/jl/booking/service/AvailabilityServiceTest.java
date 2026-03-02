package com.jl.booking.service;

import com.jl.booking.exception.ValidationException;
import com.jl.booking.model.dto.CleanerAvailabilityDto;
import com.jl.booking.model.dto.TimeSlotDto;
import com.jl.booking.model.entity.Booking;
import com.jl.booking.model.entity.Cleaner;
import com.jl.booking.model.entity.Vehicle;
import com.jl.booking.model.enums.BookingStatus;
import com.jl.booking.model.enums.DurationType;
import com.jl.booking.repository.BookingRepository;
import com.jl.booking.repository.CleanerRepository;
import com.jl.booking.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static com.jl.booking.service.AvailabilityService.WORKING_END;
import static com.jl.booking.service.AvailabilityService.WORKING_START;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CleanerRepository cleanerRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private Cleaner cleaner;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setName("Vehicle-A");

        cleaner = new Cleaner();
        cleaner.setId(1L);
        cleaner.setName("Cleaner-VA-C1");
        cleaner.setVehicle(vehicle);

        vehicle.setCleaners(List.of(cleaner));
    }

    @Test
    void getAvailabilityByDate_Friday_ThrowsException() {
        LocalDate friday = LocalDate.of(2026, 3, 6); // Friday

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> availabilityService.getAvailabilityByDate(friday))
                .withMessage("No bookings on Fridays");
    }

    @Test
    void getAvailabilityByDate_NoBookings_ReturnsFullDaySlot() {
        LocalDate monday = LocalDate.of(2026, 3, 2); // Monday

        when(bookingRepository.findByCleanerIdAndDate(anyLong(), eq(monday)))
                .thenReturn(List.of());
        when(cleanerRepository.findAll()).thenReturn(List.of(cleaner));

        List<CleanerAvailabilityDto> cleanerAvailabilityList = availabilityService.getAvailabilityByDate(monday);

        assertThat(cleanerAvailabilityList).hasSize(1);
        assertThat(cleanerAvailabilityList.getFirst().cleanerId()).isEqualTo(1L);

        // Full day slots check
        assertThat(cleanerAvailabilityList.getFirst().freeSlots()).hasSize(1);

        TimeSlotDto slot = cleanerAvailabilityList.getFirst().freeSlots().getFirst();
        assertThat(LocalTime.parse(slot.startTime())).isEqualTo(WORKING_START);
        assertThat(LocalTime.parse(slot.endTime())).isEqualTo(WORKING_END);
    }

    @Test
    void calculateFreeSlots_With30MinBreak_CorrectGaps() {
        LocalDate date = LocalDate.of(2026, 3, 2);
        Booking booking1 = createBooking(date.atTime(9, 0), date.atTime(11, 0));
        Booking booking2 = createBooking(date.atTime(16, 0), date.atTime(18, 0));

        when(bookingRepository.findByCleanerIdAndDate(anyLong(), eq(date)))
                .thenReturn(List.of(booking1, booking2));
        when(cleanerRepository.findAll()).thenReturn(List.of(cleaner));

        List<CleanerAvailabilityDto> cleanerAvailabilityList = availabilityService.getAvailabilityByDate(date);

        assertThat(cleanerAvailabilityList).hasSize(1);
        assertThat(cleanerAvailabilityList.getFirst().freeSlots()).hasSize(2);

        TimeSlotDto slot1 = cleanerAvailabilityList.getFirst().freeSlots().getFirst();
        assertThat(LocalTime.parse(slot1.startTime())).isEqualTo(LocalTime.of(11, 30));
        assertThat(LocalTime.parse(slot1.endTime())).isEqualTo(LocalTime.of(15, 30));

        TimeSlotDto slot2 = cleanerAvailabilityList.getFirst().freeSlots().getLast();
        assertThat(LocalTime.parse(slot2.startTime())).isEqualTo(LocalTime.of(18, 30));
        assertThat(LocalTime.parse(slot2.endTime())).isEqualTo(LocalTime.of(22, 00));
    }

    @Test
    void getAvailabilityByTimeSlot_OutsideWorkingHours_ThrowsException() {
        LocalDate date = LocalDate.of(2026, 3, 2);
        LocalTime tooEarly = LocalTime.of(7, 59);

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> availabilityService.getAvailabilityByTimeSlot(
                        date, tooEarly, DurationType.TWO_HOURS, 1))
                .withMessage("Booking outside working hours 08:00-22:00");
    }

    @Test
    void getAvailabilityByTimeSlot_VehicleBusy_ReturnsEmpty() {
        LocalDate date = LocalDate.of(2026, 3, 2);
        LocalDateTime testStart = date.atTime(14, 0);

        Booking overlappingBooking = createBooking(testStart.minusMinutes(15), testStart.minusMinutes(30));

        when(vehicleRepository.findAllWithCleaners()).thenReturn(List.of(vehicle));
        when(bookingRepository.findByVehicleAndDate(any(), any()))
                .thenReturn(List.of(overlappingBooking));

        var result = availabilityService.getAvailabilityByTimeSlot(date, LocalTime.of(14, 0), DurationType.TWO_HOURS, 1);

        assertThat(result).isEmpty();
    }

    @Test
    void getAvailabilityByTimeSlot_VehicleFree_CleanerFree_MatchesCleanerCount() {
        LocalDate date = LocalDate.of(2026, 3, 2);

        when(vehicleRepository.findAllWithCleaners()).thenReturn(List.of(vehicle));
        when(bookingRepository.findByVehicleAndDate(any(), any()))
                .thenReturn(List.of()); // Vehicle free
        when(bookingRepository.findByCleanerIdAndDate(1L, date)).thenReturn(List.of()); // Cleaner free

        var result = availabilityService.getAvailabilityByTimeSlot(date, LocalTime.of(10, 0), DurationType.TWO_HOURS, 1);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().vehicleId()).isEqualTo(1L);
        assertThat(result.getFirst().availableCleanerIds()).contains(1L);
    }

    @Test
    void getAvailabilityByTimeSlot_CleanerBusy_SkipsCleaner() {
        LocalDate date = LocalDate.of(2026, 3, 2);
        LocalDateTime testStart = date.atTime(10, 0);
        LocalDateTime testEnd = testStart.plusMinutes(DurationType.TWO_HOURS.getMinutes());

        Booking cleanerBooking = createBooking(testStart.minusHours(1), testEnd.plusHours(1));

        when(vehicleRepository.findAllWithCleaners()).thenReturn(List.of(vehicle));
        when(bookingRepository.findByVehicleAndDate(any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.findByCleanerIdAndDate(1L, date)).thenReturn(List.of(cleanerBooking));

        var result = availabilityService.getAvailabilityByTimeSlot(date, LocalTime.of(10, 0), DurationType.TWO_HOURS, 1);

        assertThat(result).isEmpty();
    }

    @Test
    void getAvailabilityByTimeSlot_TwoCleanersBusy_TwoAvailable_ReturnsCorrectCount() {
        LocalDate date = LocalDate.of(2026, 3, 2);

        Cleaner cleaner1 = createCleaner(1L, "C1", vehicle);
        Cleaner cleaner2 = createCleaner(2L, "C2", vehicle);
        Cleaner cleaner3 = createCleaner(3L, "C3", vehicle);
        Cleaner cleaner4 = createCleaner(4L, "C4", vehicle);
        vehicle.setCleaners(List.of(cleaner1, cleaner2, cleaner3, cleaner4));

        Booking busyBooking1 = createBooking(date.atTime(10, 0), date.atTime(12, 0));
        Booking busyBooking2 = createBooking(date.atTime(10, 0), date.atTime(12, 0));

        when(vehicleRepository.findAllWithCleaners()).thenReturn(List.of(vehicle));
        when(bookingRepository.findByVehicleAndDate(any(), any()))
                .thenReturn(List.of());

        when(bookingRepository.findByCleanerIdAndDate(1L, date)).thenReturn(List.of(busyBooking1));
        when(bookingRepository.findByCleanerIdAndDate(2L, date)).thenReturn(List.of(busyBooking2));
        when(bookingRepository.findByCleanerIdAndDate(3L, date)).thenReturn(List.of());
        when(bookingRepository.findByCleanerIdAndDate(4L, date)).thenReturn(List.of());

        var result = availabilityService.getAvailabilityByTimeSlot(date, LocalTime.of(12, 0), DurationType.TWO_HOURS, 2);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().vehicleId()).isEqualTo(1L);
        assertThat(result.getFirst().availableCleanerIds()).containsExactlyInAnyOrder(3L, 4L);
        assertThat(result.getFirst().requiredCleanerCount()).isEqualTo(2);
    }

    @Test
    void testIsVehicleFree() {

        LocalDate date = LocalDate.of(2026, 3, 2);

        // Existing bookings

        List<Booking> bookings = List.of(
                createBooking(date.atTime(8, 0), date.atTime(10, 0)),      // B1
                createBooking(date.atTime(10, 30), date.atTime(12, 30))    // B2
        );
        when(bookingRepository.findByVehicleAndDate(any(), any())).thenReturn(bookings);

        // Case 1: 8:30–10:30 → Not Allowed
        assertFalse(availabilityService.isVehicleFree(
                vehicle, date, date.atTime(8, 30), date.atTime(10, 30), null));

        // Case 2: 10:00–12:00 → Not Allowed
        assertFalse(availabilityService.isVehicleFree(
                vehicle, date, date.atTime(10, 0), date.atTime(12, 0), null));

        // Case 3: 9:00–11:00 → Allowed
        assertTrue(availabilityService.isVehicleFree(
                vehicle, date, date.atTime(9, 0), date.atTime(11, 0), null));

        // Case 4: 9:30–11:30 → Allowed
        assertTrue(availabilityService.isVehicleFree(
                vehicle, date, date.atTime(9, 30), date.atTime(11, 30), null));

        // Case 4: 12-14 → Allowed
        assertTrue(availabilityService.isVehicleFree(
                vehicle, date, date.atTime(12, 0), date.atTime(14, 0), null));

        // Case 5: 12:30-14:30 -> Not allowed
        assertFalse(availabilityService.isVehicleFree(
                vehicle, date, date.atTime(12, 30), date.atTime(14, 0), null));
    }

    private Cleaner createCleaner(Long id, String name, Vehicle vehicle) {
        Cleaner cleaner = new Cleaner();
        cleaner.setId(id);
        cleaner.setName(name);
        cleaner.setVehicle(vehicle);
        return cleaner;
    }

    private Booking createBooking(LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setDurationMinutes(120);
        booking.setCleanerCount(1);
        booking.setVehicle(vehicle);
        return booking;
    }
}

