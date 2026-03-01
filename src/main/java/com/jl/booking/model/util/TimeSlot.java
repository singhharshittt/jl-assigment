package com.jl.booking.model.util;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.ArrayList;

public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot withBuffer(Duration buffer) {
        return new TimeSlot(start.minus(buffer), end.plus(buffer));
    }

    public List<TimeSlot> subtract(TimeSlot other) {
        List<TimeSlot> result = new ArrayList<>();

        if (this.end.isBefore(other.start) || this.start.isAfter(other.end)) {
            result.add(this);
        } else {
            if (this.start.isBefore(other.start)) {
                result.add(new TimeSlot(this.start, other.start));
            }
            if (this.end.isAfter(other.end)) {
                result.add(new TimeSlot(other.end, this.end));
            }
        }
        return result;
    }

    public Duration duration() {
        return Duration.between(start, end);
    }
}

