package com.jl.booking.model.dto;

import com.jl.booking.model.entity.Cleaner;
import com.jl.booking.model.util.TimeSlot;
import lombok.Builder;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record CleanerAvailabilityDto(
        Long cleanerId,
        String cleanerName,
        Long vehicleId,
        String vehicleName,
        List<TimeSlotDto> freeSlots
) {
    public CleanerAvailabilityDto(Cleaner cleaner, List<TimeSlot> slots) {
        this(cleaner.getId(), cleaner.getName(),
                cleaner.getVehicle().getId(), cleaner.getVehicle().getName(),
                slots.stream().map(TimeSlotDto::from).collect(Collectors.toList()));
    }
}

