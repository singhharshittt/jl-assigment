package com.jl.booking.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
public record BookingResponseDto(
        @Schema(description = "Booking ID")
        Long bookingId,

        @Schema(description = "Start datetime")
        String startDateTime,

        @Schema(description = "End datetime")
        String endDateTime,

        @Schema(description = "Vehicle assigned")
        Long vehicleId,

        @Schema(description = "Assigned cleaners")
        List<Long> cleanerIds
) {}