package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;

/**
 * The price of a stay: every night costs the room's nightly rate, plus the high-season surcharge on
 * high-season nights; the rate plan may then give a discount on the sum. Money is rounded to cents
 * (half-even) at every step.
 */
public final class PriceCalculator {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");
    static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    static final BigDecimal CORPORATE_TECNOSUL_DISCOUNT = new BigDecimal("0.12");
    static final BigDecimal CORPORATE_AGROVALE_DISCOUNT = new BigDecimal("0.18");
    static final BigDecimal CORPORATE_FARMAVIDA_DISCOUNT = new BigDecimal("0.25");
    static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = switch (ratePlan) {
            case STANDARD -> cents(BigDecimal.ZERO);
            case NON_REFUNDABLE -> cents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
            case CORPORATE -> cents(subtotal.multiply(getCorporateDiscount(companyCode)));
            case LONG_STAY -> cents(subtotal.multiply(LONG_STAY_DISCOUNT));
            case WEEKEND_PACKAGE -> cents(BigDecimal.ZERO);
        };
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    public Price calculateWeekendPackage(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal packagePrice = switch (room.type()) {
            case STANDARD -> new BigDecimal("520.00");
            case DELUXE -> new BigDecimal("820.00");
            case SUITE -> new BigDecimal("1200.00");
        };
        return new Price(2, packagePrice, cents(BigDecimal.ZERO), packagePrice);
    }

    private static BigDecimal getCorporateDiscount(String companyCode) {
        return switch (companyCode) {
            case "TECNOSUL" -> CORPORATE_TECNOSUL_DISCOUNT;
            case "AGROVALE" -> CORPORATE_AGROVALE_DISCOUNT;
            case "FARMAVIDA" -> CORPORATE_FARMAVIDA_DISCOUNT;
            default -> throw new IllegalArgumentException("Invalid company code: " + companyCode);
        };
    }

    BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal nightlyRate = room.nightlyRate();
        if (isHighSeason(night)) {
            return cents(nightlyRate.multiply(BigDecimal.ONE.add(HIGH_SEASON_SURCHARGE)));
        }
        return cents(nightlyRate);
    }

    /** High season: from December 15 to the end of February, and the whole of July. */
    static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }

    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    static boolean isWeekendPackageValid(LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.minusDays(1).equals(checkIn.plusDays(1))) {
            return false;
        }
        return checkIn.getDayOfWeek() == DayOfWeek.FRIDAY && checkOut.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
