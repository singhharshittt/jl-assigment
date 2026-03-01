package com.jl.booking.model.dto;

import com.jl.booking.model.util.TimeSlot;
import lombok.Builder;

@Builder
public record TimeSlotDto(String startTime, String endTime) {

    public static TimeSlotDto from(TimeSlot slot) {
        return new TimeSlotDto(
                slot.start().toLocalTime().toString(),
                slot.end().toLocalTime().toString()
        );
    }
}
