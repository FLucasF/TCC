package hotel.reservations;

import hotel.rooms.Room;
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
    static final BigDecimal WEEKEND_PACKAGE_DISCOUNT = new BigDecimal("0.50");

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        return calculate(room, checkIn, checkOut, ratePlan, null);
    }

    public Price calculate(
            Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {
        if (ratePlan == RatePlan.WEEKEND_PACKAGE) {
            return calculateWeekendPackage(room, checkIn, checkOut);
        }
        if (ratePlan == RatePlan.LONG_STAY) {
            return calculateLongStay(room, checkIn, checkOut);
        }
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = switch (ratePlan) {
            case STANDARD -> cents(BigDecimal.ZERO);
            case NON_REFUNDABLE -> cents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
            case CORPORATE -> cents(subtotal.multiply(corporateDiscount(companyCode)));
            case WEEKEND_PACKAGE, LONG_STAY -> cents(BigDecimal.ZERO);
        };
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private Price calculateWeekendPackage(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal packagePrice = weekendPackagePrice(room.type());
        return new Price(2, packagePrice, cents(BigDecimal.ZERO), packagePrice);
    }

    private Price calculateLongStay(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = cents(subtotal.multiply(LONG_STAY_DISCOUNT));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private BigDecimal corporateDiscount(String companyCode) {
        return switch (companyCode) {
            case "TECNOSUL" -> CORPORATE_TECNOSUL_DISCOUNT;
            case "AGROVALE" -> CORPORATE_AGROVALE_DISCOUNT;
            case "FARMAVIDA" -> CORPORATE_FARMAVIDA_DISCOUNT;
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal weekendPackagePrice(hotel.rooms.RoomType roomType) {
        return switch (roomType) {
            case STANDARD -> new BigDecimal("520.00");
            case DELUXE -> new BigDecimal("820.00");
            case SUITE -> new BigDecimal("1200.00");
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
}
