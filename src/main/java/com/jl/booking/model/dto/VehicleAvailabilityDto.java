package com.jl.booking.model.dto;

import com.jl.booking.model.entity.Cleaner;
import com.jl.booking.model.entity.Vehicle;
import lombok.Builder;

import java.util.List;
import java.util.stream.Collectors;

@Builder
public record VehicleAvailabilityDto(
        Long vehicleId,
        String vehicleName,
        List<Long> availableCleanerIds,
        int requiredCleanerCount
) {
    public VehicleAvailabilityDto(Vehicle vehicle, List<Cleaner> cleaners, int cleanerCount) {
        this(vehicle.getId(), vehicle.getName(),
                cleaners.stream().map(Cleaner::getId).collect(Collectors.toList()),
                cleanerCount);
    }
}
