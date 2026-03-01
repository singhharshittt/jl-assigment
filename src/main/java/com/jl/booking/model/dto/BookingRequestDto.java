package com.jl.booking.model.dto;

import com.jl.booking.model.enums.DurationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record BookingRequestDto(
        @NotNull @Schema(description = "Booking date", example = "2026-03-02")
        LocalDate date,

        @NotNull @Schema(description = "Start time", example = "14:00")
        LocalTime startTime,

        @NotNull @Schema(description = "Duration", example = "TWO_HOURS")
        DurationType durationHours,

        @NotNull @Min(1) @Max(3) @Schema(description = "Number of cleaners", example = "2")
        Integer cleanerCount
) {}

