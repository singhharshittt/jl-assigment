package com.jl.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jl.booking.exception.ConflictException;
import com.jl.booking.exception.NotFoundException;
import com.jl.booking.model.dto.BookingRequestDto;
import com.jl.booking.model.dto.BookingResponseDto;
import com.jl.booking.model.dto.BookingUpdateRequestDto;
import com.jl.booking.model.enums.DurationType;
import com.jl.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private BookingService bookingService;

    // 1. Success: createBooking returns 201
    @Test
    void createBooking_success() throws Exception {
        BookingRequestDto request = BookingRequestDto.builder()
                .date(LocalDate.of(2025, 10, 24))
                .startTime(LocalTime.of(10, 0))
                .durationHours(DurationType.TWO_HOURS)
                .cleanerCount(2)
                .build();

        BookingResponseDto response = new BookingResponseDto(
                100L,
                "2025-10-24T10:00",
                "2025-10-24T12:00",
                1L,
                List.of(1L, 2L)
        );

        Mockito.when(bookingService.createBooking(any())).thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingId").value(100L))
                .andExpect(jsonPath("$.vehicleId").value(1L))
                .andExpect(jsonPath("$.cleanerIds[0]").value(1L));
    }

    @Test
    void createBooking_failure_conflict() throws Exception {
        BookingRequestDto request = BookingRequestDto.builder()
                .date(LocalDate.of(2025, 10, 24))
                .startTime(LocalTime.of(10, 0))
                .durationHours(DurationType.TWO_HOURS)
                .cleanerCount(2)
                .build();

        Mockito.when(bookingService.createBooking(any()))
                .thenThrow(new ConflictException("No available vehicle with sufficient cleaners"));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("No available vehicle with sufficient cleaners"));
    }

    // 3. Success: updateBooking returns 200
    @Test
    void updateBooking_success() throws Exception {
        BookingUpdateRequestDto request = BookingUpdateRequestDto.builder()
                .date(LocalDate.of(2025, 10, 25))
                .startTime(LocalTime.of(14, 0))
                .build();

        BookingResponseDto response = new BookingResponseDto(
                101L,
                "2025-10-25T14:00",
                "2025-10-25T16:00",
                1L,
                List.of(1L, 2L)
        );

        Mockito.when(bookingService.updateBooking(eq(101L), any())).thenReturn(response);

        mockMvc.perform(put("/api/bookings/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(101L))
                .andExpect(jsonPath("$.vehicleId").value(1L));
    }

    // 4. Failure: updateBooking throws RuntimeException → 404
    @Test
    void updateBooking_failure_notFound() throws Exception {
        BookingUpdateRequestDto request = BookingUpdateRequestDto.builder()
                .date(LocalDate.of(2025, 10, 25))
                .startTime(LocalTime.of(14, 0))
                .build();

        Mockito.when(bookingService.updateBooking(eq(999L), any()))
                .thenThrow(new NotFoundException("Booking not found: 999"));

        mockMvc.perform(put("/api/bookings/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Booking not found: 999"));
    }

}
