"""Scenario 02 (negative control): the contained solution (a constant and a list of dates) and the over-engineered
one (a Season interface with a class per season)."""

from kit import CALCULATOR, CALCULATOR_TEST, HTTP_TEST, Solution


def update_tests(solution: Solution) -> None:
    solution.replace(CALCULATOR_TEST, "// December 14: 249.90; December 15: 249.90 + 20% = 299.88",
                     "// December 14: 249.90; December 15: 249.90 + 25% = 312.38")
    solution.replace(CALCULATOR_TEST, '        assertEquals(new BigDecimal("549.78"), price.total());',
                     '        assertEquals(new BigDecimal("562.28"), price.total());', count=2)
    solution.replace(CALCULATOR_TEST, "// February 29 (leap year): 299.88; March 1: 249.90",
                     "// February 29 (leap year): 312.38; March 1: 249.90")
    solution.replace(CALCULATOR_TEST, """        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());""", """        // 4 nights of 579.90 + 25% = 724.88 each
        assertEquals(new BigDecimal("2899.52"), price.subtotal());
        assertEquals(new BigDecimal("434.93"), price.discount());
        assertEquals(new BigDecimal("2464.59"), price.total());""")
    solution.replace(CALCULATOR_TEST, """    @Test
    void nonRefundableTakes15PercentOffRoundingHalfEven() {""", """    @Test
    void easterWeekIsHighSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 20)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 21)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 28)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 29)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2028, 4, 8)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2028, 4, 9)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2028, 4, 16)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2028, 4, 17)));
    }

    @Test
    void nonRefundableTakes15PercentOffRoundingHalfEven() {""")
    solution.replace(HTTP_TEST, "assertEquals(935.76, reservation.get(\"total\").asDouble());",
                     "assertEquals(974.76, reservation.get(\"total\").asDouble());")
    solution.replace(HTTP_TEST, 'endsWith("total,935.76\\n")', 'endsWith("total,974.76\\n")')


contained = Solution("02-contained")
contained.replace(CALCULATOR, "import java.time.Month;\n", "import java.time.Month;\nimport java.util.List;\n")
contained.replace(CALCULATOR, 'static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");',
                  'static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.25");')
contained.replace(CALCULATOR, """    static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
""", """    static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    /** Easter week, from Palm Sunday to Easter Sunday (both nights included). */
    private static final List<DateRange> EASTER_WEEKS = List.of(
            new DateRange(LocalDate.of(2027, 3, 21), LocalDate.of(2027, 3, 28)),
            new DateRange(LocalDate.of(2028, 4, 9), LocalDate.of(2028, 4, 16)));

    private record DateRange(LocalDate first, LocalDate last) {
        boolean contains(LocalDate date) {
            return !date.isBefore(first) && !date.isAfter(last);
        }
    }
""")
contained.replace(CALCULATOR, """    /** High season: from December 15 to the end of February, and the whole of July. */""",
                  """    /** High season: from December 15 to the end of February, the whole of July, and Easter week. */""")
contained.replace(CALCULATOR, """                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);""",
                  """                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15)
                || EASTER_WEEKS.stream().anyMatch(week -> week.contains(date));""")
update_tests(contained)
contained.build_and_save()


over = Solution("02-overengineered")
SEASONS = "src/main/java/hotel/reservations/seasons/"
over.write(SEASONS + "Season.java", """package hotel.reservations.seasons;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A period of the year with its own surcharge on the nightly rate. */
public interface Season {

    boolean includes(LocalDate night);

    BigDecimal surcharge();
}
""")
over.write(SEASONS + "SummerSeason.java", """package hotel.reservations.seasons;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** From December 15 to the end of February. */
public final class SummerSeason implements Season {

    @Override
    public boolean includes(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    @Override
    public BigDecimal surcharge() {
        return new BigDecimal("0.25");
    }
}
""")
over.write(SEASONS + "JulyHolidays.java", """package hotel.reservations.seasons;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** The whole of July. */
public final class JulyHolidays implements Season {

    @Override
    public boolean includes(LocalDate night) {
        return night.getMonth() == Month.JULY;
    }

    @Override
    public BigDecimal surcharge() {
        return new BigDecimal("0.25");
    }
}
""")
over.write(SEASONS + "EasterWeek.java", """package hotel.reservations.seasons;

import java.math.BigDecimal;
import java.time.LocalDate;

/** From Palm Sunday to Easter Sunday, both nights included. */
public final class EasterWeek implements Season {

    private final LocalDate palmSunday;
    private final LocalDate easterSunday;

    public EasterWeek(LocalDate palmSunday, LocalDate easterSunday) {
        this.palmSunday = palmSunday;
        this.easterSunday = easterSunday;
    }

    @Override
    public boolean includes(LocalDate night) {
        return !night.isBefore(palmSunday) && !night.isAfter(easterSunday);
    }

    @Override
    public BigDecimal surcharge() {
        return new BigDecimal("0.25");
    }
}
""")
over.write(SEASONS + "SeasonCalendar.java", """package hotel.reservations.seasons;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** All the seasons of the inn; a new season is one more entry here. */
public final class SeasonCalendar {

    private final List<Season> seasons = List.of(
            new SummerSeason(),
            new JulyHolidays(),
            new EasterWeek(LocalDate.of(2027, 3, 21), LocalDate.of(2027, 3, 28)),
            new EasterWeek(LocalDate.of(2028, 4, 9), LocalDate.of(2028, 4, 16)));

    public Optional<Season> seasonOf(LocalDate night) {
        return seasons.stream().filter(season -> season.includes(night)).findFirst();
    }
}
""")
over.replace(CALCULATOR, "import hotel.rooms.Room;\n",
             "import hotel.reservations.seasons.Season;\nimport hotel.reservations.seasons.SeasonCalendar;\n"
             "import hotel.rooms.Room;\n")
over.replace(CALCULATOR, "import java.time.Month;\n", "import java.util.Optional;\n")
over.replace(CALCULATOR, """    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");
""", """    private static final SeasonCalendar SEASONS = new SeasonCalendar();
""")
over.replace(CALCULATOR, """        BigDecimal nightlyRate = room.nightlyRate();
        if (isHighSeason(night)) {
            return cents(nightlyRate.multiply(BigDecimal.ONE.add(HIGH_SEASON_SURCHARGE)));
        }
        return cents(nightlyRate);""", """        BigDecimal nightlyRate = room.nightlyRate();
        Optional<Season> season = SEASONS.seasonOf(night);
        if (season.isPresent()) {
            return cents(nightlyRate.multiply(BigDecimal.ONE.add(season.get().surcharge())));
        }
        return cents(nightlyRate);""")
over.replace(CALCULATOR, """    /** High season: from December 15 to the end of February, and the whole of July. */
    static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }""", """    static boolean isHighSeason(LocalDate date) {
        return SEASONS.seasonOf(date).isPresent();
    }""")
update_tests(over)
over.build_and_save()
