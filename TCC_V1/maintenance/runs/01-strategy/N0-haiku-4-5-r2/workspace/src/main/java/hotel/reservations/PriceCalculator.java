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
        return switch (ratePlan) {
            case STANDARD -> calculateStandard(room, checkIn, checkOut);
            case NON_REFUNDABLE -> calculateNonRefundable(room, checkIn, checkOut);
            case CORPORATE -> calculateCorporate(room, checkIn, checkOut, companyCode);
            case WEEKEND_PACKAGE -> calculateWeekendPackage(room, checkIn, checkOut);
            case LONG_STAY -> calculateLongStay(room, checkIn, checkOut);
        };
    }

    private Price calculateStandard(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        return new Price(nights, subtotal, cents(BigDecimal.ZERO), subtotal);
    }

    private Price calculateNonRefundable(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = cents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private Price calculateCorporate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discountRate = switch (companyCode) {
            case "TECNOSUL" -> CORPORATE_TECNOSUL_DISCOUNT;
            case "AGROVALE" -> CORPORATE_AGROVALE_DISCOUNT;
            case "FARMAVIDA" -> CORPORATE_FARMAVIDA_DISCOUNT;
            default -> null;
        };
        if (discountRate == null) {
            throw new IllegalArgumentException("Invalid company code: " + companyCode);
        }
        BigDecimal discount = cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private Price calculateWeekendPackage(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = checkIn.until(checkOut).getDays();
        if (nights != 2) {
            throw new IllegalArgumentException("Weekend package requires exactly 2 nights");
        }
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Weekend package requires Friday to Sunday");
        }
        BigDecimal packagePrice = switch (room.type()) {
            case STANDARD -> new BigDecimal("520.00");
            case DELUXE -> new BigDecimal("820.00");
            case SUITE -> new BigDecimal("1200.00");
        };
        return new Price(nights, packagePrice, cents(BigDecimal.ZERO), packagePrice);
    }

    private Price calculateLongStay(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = checkIn.until(checkOut).getDays();
        if (nights < 7) {
            throw new IllegalArgumentException("Long stay requires at least 7 nights");
        }
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
        }
        BigDecimal discount = cents(subtotal.multiply(LONG_STAY_DISCOUNT));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
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
