package com.jl.booking.controller;

import com.jl.booking.model.dto.*;
import com.jl.booking.model.enums.DurationType;
import com.jl.booking.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
@Tag(name = "Availability API", description = "Check cleaner availability for bookings")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    @Operation(
            summary = "Get availability by date or specific time slot",
            description = """
            **Mode 1:** date only → Returns per-cleaner free time slots (≥2hr)
            **Mode 2:** date + startTime + durationHours + cleanerCount → Returns available vehicles with sufficient cleaners
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Availability found",
                    content = {
                            @Content(mediaType = "application/json",
                                    schema = @Schema(oneOf = {CleanerAvailabilityDto[].class, VehicleAvailabilityDto[].class}))
                    }),
            @ApiResponse(responseCode = "400", description = "Invalid date/time or Friday")
    })
    public ResponseEntity<List<?>> getAvailability(
            @Parameter(description = "Date in yyyy-MM-dd format", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,

            @Parameter(description = "Start time HH:mm (Mode 2 only)")
            @RequestParam(required = false) LocalTime startTime,

            @Parameter(description = "Duration: TWO_HOURS or FOUR_HOURS (Mode 2 only)")
            @RequestParam(required = false) DurationType durationHours,

            @Parameter(description = "Number of cleaners needed (1-3, Mode 2 only)")
            @RequestParam(required = false) Integer cleanerCount) {

        if (startTime == null || durationHours == null || cleanerCount == null) {
            // Mode 1: Date only
            var result = availabilityService.getAvailabilityByDate(date);
            return ResponseEntity.ok(result);
        }

        // Mode 2: Full time slot query
        var result = availabilityService.getAvailabilityByTimeSlot(date, startTime, durationHours, cleanerCount);
        return ResponseEntity.ok(result);
    }
}

