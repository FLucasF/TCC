package hotel.reservations;

import java.time.LocalDate;
import java.time.Month;

/** A period of the year that counts as high season. */
@FunctionalInterface
public interface Season {

    boolean includes(LocalDate night);

    /** Every night of the month, in every year. */
    static Season wholeMonth(Month month) {
        return night -> night.getMonth() == month;
    }

    /** From the given day of the first month to the end of the last month, in every year (may wrap the new year). */
    static Season yearly(Month firstMonth, int firstDay, Month lastMonth) {
        return night -> {
            Month month = night.getMonth();
            boolean afterStart = month == firstMonth && night.getDayOfMonth() >= firstDay;
            boolean wraps = firstMonth.compareTo(lastMonth) > 0;
            int from = firstMonth.getValue();
            int to = lastMonth.getValue();
            int current = month.getValue();
            boolean inBetween = wraps ? (current > from || current <= to) : (current > from && current <= to);
            return afterStart || inBetween;
        };
    }

    /** The nights from {@code first} to {@code last}, both included, in that one year. */
    static Season between(LocalDate first, LocalDate last) {
        return night -> !night.isBefore(first) && !night.isAfter(last);
    }
}
