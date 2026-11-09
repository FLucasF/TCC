package hotel.reservations;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;

/**
 * The high-season nights: from December 15 to the end of February and the whole of July, every year,
 * plus the Easter weeks (Palm Sunday to Easter Sunday, both included) of the years listed here.
 */
final class HighSeason {

    private record YearlyPeriod(MonthDay first, MonthDay last) {
        boolean contains(LocalDate date) {
            MonthDay day = MonthDay.from(date);
            return !day.isBefore(first) && !day.isAfter(last);
        }
    }

    private record Period(LocalDate first, LocalDate last) {
        boolean contains(LocalDate date) {
            return !date.isBefore(first) && !date.isAfter(last);
        }
    }

    private static final List<YearlyPeriod> YEARLY = List.of(
            new YearlyPeriod(MonthDay.of(1, 1), MonthDay.of(2, 29)),
            new YearlyPeriod(MonthDay.of(7, 1), MonthDay.of(7, 31)),
            new YearlyPeriod(MonthDay.of(12, 15), MonthDay.of(12, 31)));

    private static final List<Period> EASTER_WEEKS = List.of(
            new Period(LocalDate.of(2027, 3, 21), LocalDate.of(2027, 3, 28)),
            new Period(LocalDate.of(2028, 4, 9), LocalDate.of(2028, 4, 16)));

    private HighSeason() {
    }

    static boolean includes(LocalDate night) {
        return YEARLY.stream().anyMatch(period -> period.contains(night))
                || EASTER_WEEKS.stream().anyMatch(period -> period.contains(night));
    }
}
