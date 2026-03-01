package com.jl.booking.controller;

import com.jl.booking.model.dto.*;
import com.jl.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Booking API", description = "Create and manage cleaning service bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(summary = "Create new booking", description = "Auto-selects available vehicle and cleaners")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created"),
            @ApiResponse(responseCode = "400", description = "Invalid input or no availability"),
            @ApiResponse(responseCode = "409", description = "Conflict - no available vehicle/cleaners")
    })
    public ResponseEntity<BookingResponseDto> createBooking(@Valid @RequestBody BookingRequestDto request) {
        var booking = bookingService.createBooking(request);
        return ResponseEntity.status(201).body(booking);
    }

    @PutMapping("/{bookingId}")
    @Operation(summary = "Update booking date/time")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking updated"),
            @ApiResponse(responseCode = "400", description = "Invalid update or no availability"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    public ResponseEntity<BookingResponseDto> updateBooking(
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingUpdateRequestDto request) {
        var booking = bookingService.updateBooking(bookingId, request);
        return ResponseEntity.ok(booking);
    }
}

