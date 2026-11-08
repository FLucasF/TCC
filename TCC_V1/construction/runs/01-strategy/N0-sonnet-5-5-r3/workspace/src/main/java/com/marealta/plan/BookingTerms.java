package com.marealta.plan;

import com.marealta.Room;
import java.time.LocalDate;

public record BookingTerms(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    public int nights() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
