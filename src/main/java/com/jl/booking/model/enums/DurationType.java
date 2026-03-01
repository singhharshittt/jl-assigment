package com.jl.booking.model.enums;

public enum DurationType {
    TWO_HOURS(120), FOUR_HOURS(240);

    private final int minutes;

    DurationType(int minutes) {
        this.minutes = minutes;
    }

    public static DurationType valueOfMinutes(Integer durationMinutes) {
        return durationMinutes == 120 ? TWO_HOURS : FOUR_HOURS;
    }

    public int getMinutes() {
        return minutes;
    }
}

