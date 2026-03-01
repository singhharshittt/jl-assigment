package com.jl.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jl.booking.exception.ValidationException;
import com.jl.booking.model.dto.CleanerAvailabilityDto;
import com.jl.booking.model.dto.TimeSlotDto;
import com.jl.booking.model.dto.VehicleAvailabilityDto;
import com.jl.booking.model.enums.DurationType;
import com.jl.booking.service.AvailabilityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = AvailabilityController.class)
class AvailabilityControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean
    private AvailabilityService availabilityService;

    //Mode 1: Success - date only returns cleaner availability
    @Test
    void getAvailability_dateOnly_success() throws Exception {
        CleanerAvailabilityDto dto = CleanerAvailabilityDto.builder()
                .cleanerId(1L)
                .freeSlots(List.of(new TimeSlotDto("08:00", "12:00")))
                .build();

        Mockito.when(availabilityService.getAvailabilityByDate(any(LocalDate.class)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/availability")
                        .param("date", "2025-10-24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cleanerId").value(1L))
                .andExpect(jsonPath("$[0].freeSlots[0].startTime").value("08:00"));
    }

    //Mode 2: Success - full time slot query returns vehicle availability
    @Test
    void getAvailability_timeSlot_success() throws Exception {
        VehicleAvailabilityDto dto = VehicleAvailabilityDto.builder()
                .vehicleId(1L)
                .availableCleanerIds(List.of(1L, 2L))
                .requiredCleanerCount(2)
                .build();

        Mockito.when(availabilityService.getAvailabilityByTimeSlot(
                        any(LocalDate.class), any(LocalTime.class), any(DurationType.class), anyInt()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/availability")
                        .param("date", "2025-10-24")
                        .param("startTime", "10:00")
                        .param("durationHours", "TWO_HOURS")
                        .param("cleanerCount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].vehicleId").value(1L))
                .andExpect(jsonPath("$[0].availableCleanerIds[0]").value(1L));
    }

    //Failure: Non-working day (Friday) → ValidationException → 400
    @Test
    void getAvailability_failure_nonWorkingDay() throws Exception {
        Mockito.when(availabilityService.getAvailabilityByDate(any(LocalDate.class)))
                .thenThrow(new ValidationException("No bookings on Fridays"));

        mockMvc.perform(get("/api/availability")
                        .param("date", "2025-10-24"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No bookings on Fridays"));
    }

    //Failure: Outside working hours → ValidationException → 400
    @Test
    void getAvailability_failure_outsideWorkingHours() throws Exception {
        Mockito.when(availabilityService.getAvailabilityByTimeSlot(
                        any(LocalDate.class), any(LocalTime.class), any(DurationType.class), anyInt()))
                .thenThrow(new ValidationException("Booking outside working hours 08:00-22:00"));

        mockMvc.perform(get("/api/availability")
                        .param("date", "2025-10-24")
                        .param("startTime", "23:00")
                        .param("durationHours", "TWO_HOURS")
                        .param("cleanerCount", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Booking outside working hours 08:00-22:00"));
    }
}
