package hotel.reservations;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.function.Predicate;

/**
 * The high-season nights: from December 15 to the end of February, the whole of July, and Easter
 * week (from Palm Sunday to Easter Sunday, both included). A new season is one more entry in
 * {@link #SEASONS}.
 */
final class HighSeason {

    private static final List<Predicate<LocalDate>> SEASONS = List.of(
            HighSeason::isSummer,
            HighSeason::isJuly,
            HighSeason::isEasterWeek);

    private HighSeason() {}

    static boolean includes(LocalDate night) {
        return SEASONS.stream().anyMatch(season -> season.test(night));
    }

    private static boolean isSummer(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }

    private static boolean isJuly(LocalDate date) {
        return date.getMonth() == Month.JULY;
    }

    private static boolean isEasterWeek(LocalDate date) {
        LocalDate easter = easterSunday(date.getYear());
        return !date.isBefore(easter.minusDays(7)) && !date.isAfter(easter);
    }

    /** Easter Sunday in the Gregorian calendar (anonymous Gregorian algorithm). */
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
