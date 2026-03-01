package com.jl.booking.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record BookingUpdateRequestDto(
        @NotNull LocalDate date,
        @NotNull LocalTime startTime
) {}



