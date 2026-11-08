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
    static final BigDecimal CORPORATE_TECNOSUL = new BigDecimal("0.12");
    static final BigDecimal CORPORATE_AGROVALE = new BigDecimal("0.18");
    static final BigDecimal CORPORATE_FARMAVIDA = new BigDecimal("0.25");
    static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");

    public Price calculate(
            Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {
        return switch (ratePlan) {
            case STANDARD, NON_REFUNDABLE -> calculateStandard(room, checkIn, checkOut, ratePlan);
            case CORPORATE -> calculateCorporate(room, checkIn, checkOut, companyCode);
            case WEEKEND_PACKAGE -> calculateWeekendPackage(room, checkIn, checkOut);
            case LONG_STAY -> calculateLongStay(room, checkIn, checkOut);
        };
    }

    private Price calculateStandard(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = switch (ratePlan) {
            case STANDARD -> cents(BigDecimal.ZERO);
            case NON_REFUNDABLE -> cents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
            default -> throw new AssertionError();
        };
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private Price calculateCorporate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discountRate = discountForCompany(companyCode);
        BigDecimal discount = cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private Price calculateWeekendPackage(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal price = packagePrice(room, nights);
        return new Price(nights, price, cents(BigDecimal.ZERO), price);
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

    private BigDecimal discountForCompany(String code) {
        if (code == null) {
            throw new IllegalArgumentException("company code is required for CORPORATE plan");
        }
        return switch (code) {
            case "TECNOSUL" -> CORPORATE_TECNOSUL;
            case "AGROVALE" -> CORPORATE_AGROVALE;
            case "FARMAVIDA" -> CORPORATE_FARMAVIDA;
            default -> throw new IllegalArgumentException("invalid company code: " + code);
        };
    }

    private BigDecimal packagePrice(Room room, int nights) {
        return switch (room.type().name()) {
            case "STANDARD" -> cents(new BigDecimal("520.00"));
            case "DELUXE" -> cents(new BigDecimal("820.00"));
            case "SUITE" -> cents(new BigDecimal("1200.00"));
            default -> throw new AssertionError("unknown room type: " + room.type());
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
