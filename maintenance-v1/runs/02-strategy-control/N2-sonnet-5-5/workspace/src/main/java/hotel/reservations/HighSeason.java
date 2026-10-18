package hotel.reservations;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;

/**
 * The high season: from December 15 to the end of February, the whole of July, and the week of Easter.
 * To add another period, add it to {@link #SEASONS}.
 */
final class HighSeason {

    private static final List<Season> SEASONS = List.of(
            night -> fixed(night, MonthDay.of(12, 15), MonthDay.of(12, 31)),
            night -> fixed(night, MonthDay.of(1, 1), MonthDay.of(2, 29)),
            night -> fixed(night, MonthDay.of(7, 1), MonthDay.of(7, 31)),
            HighSeason::easterWeek);

    private HighSeason() {}

    static boolean includes(LocalDate night) {
        return SEASONS.stream().anyMatch(season -> season.includes(night));
    }

    private static boolean fixed(LocalDate night, MonthDay first, MonthDay last) {
        MonthDay day = MonthDay.from(night);
        return !day.isBefore(first) && !day.isAfter(last);
    }

    /** The nights from Palm Sunday to Easter Sunday, both included. */
    private static boolean easterWeek(LocalDate night) {
        LocalDate easter = easterSunday(night.getYear());
        return !night.isBefore(easter.minusDays(7)) && !night.isAfter(easter);
    }

    /** Easter Sunday of the Gregorian calendar (Meeus/Jones/Butcher algorithm). */
    static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(year, month, day);
    }
}
