package br.com.marealta.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public DateRange {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Dates cannot be null");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }
    }

    public int nights() {
        return (int) checkIn.datesUntil(checkOut).count();
    }

    public List<LocalDate> getNights() {
        List<LocalDate> nights = new ArrayList<>();
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            nights.add(current);
            current = current.plusDays(1);
        }
        return nights;
    }

    public boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        if (month == Month.JULY) {
            return true;
        }

        if (month == Month.DECEMBER && day >= 15) {
            return true;
        }

        if (month == Month.JANUARY || month == Month.FEBRUARY) {
            return true;
        }

        return false;
    }

    public Money calculateNightlyTotal(Money baseRate) {
        Money total = Money.zero();
        for (LocalDate night : getNights()) {
            Money nightRate = baseRate;
            if (isHighSeason(night)) {
                nightRate = nightRate.multiply(1.20);
            }
            total = total.add(nightRate);
        }
        return total;
    }

    public boolean isFridayToSunday() {
        return checkIn.getDayOfWeek() == DayOfWeek.FRIDAY
            && checkOut.getDayOfWeek() == DayOfWeek.SUNDAY
            && nights() == 2;
    }

    public boolean overlaps(DateRange other) {
        return this.checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(this.checkOut);
    }
}
