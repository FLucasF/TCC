package hotel.reservations;

import java.time.LocalDate;
import java.time.MonthDay;

/** A period of the year in which nights are high-season nights. */
@FunctionalInterface
interface Season {

    boolean includes(LocalDate night);

    /** A period that repeats every year, from {@code start} to {@code end} inclusive; it may cross New Year. */
    static Season everyYear(MonthDay start, MonthDay end) {
        return night -> {
            MonthDay day = MonthDay.from(night);
            return start.isAfter(end)
                    ? !day.isBefore(start) || !day.isAfter(end)
                    : !day.isBefore(start) && !day.isAfter(end);
        };
    }

    /** A single period, from {@code first} to {@code last} inclusive. */
    static Season between(LocalDate first, LocalDate last) {
        return night -> !night.isBefore(first) && !night.isAfter(last);
    }
}
