package com.pousada.marealta.domain.shared;

import java.time.LocalDate;
import java.util.Objects;

public class DateRange {
    private final LocalDate checkIn;
    private final LocalDate checkOut;

    public DateRange(LocalDate checkIn, LocalDate checkOut) {
        Objects.requireNonNull(checkIn, "Check-in date cannot be null");
        Objects.requireNonNull(checkOut, "Check-out date cannot be null");
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("Check-in must be before check-out");
        }
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public LocalDate checkIn() {
        return checkIn;
    }

    public LocalDate checkOut() {
        return checkOut;
    }

    public int nights() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public boolean overlaps(DateRange other) {
        return !this.checkOut.isAfter(other.checkIn) == false &&
               !this.checkIn.isBefore(other.checkOut) == false;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(checkIn) && date.isBefore(checkOut);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DateRange)) return false;
        DateRange dateRange = (DateRange) o;
        return checkIn.equals(dateRange.checkIn) && checkOut.equals(dateRange.checkOut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkIn, checkOut);
    }

    @Override
    public String toString() {
        return checkIn + " to " + checkOut + " (" + nights() + " nights)";
    }
}
