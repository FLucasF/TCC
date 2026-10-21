package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

/**
 * The price of a stay: every night costs the room's nightly rate, plus the high-season surcharge on
 * high-season nights; the rate plan may then give a discount on the sum. Money is rounded to cents
 * (half-even) at every step.
 */
public final class PriceCalculator {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");
    static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");

    static final Map<String, BigDecimal> CORPORATE_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25")
    );

    static final Map<String, BigDecimal> WEEKEND_PACKAGE_PRICES = Map.of(
            "STANDARD", new BigDecimal("520.00"),
            "DELUXE", new BigDecimal("820.00"),
            "SUITE", new BigDecimal("1200.00")
    );

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {
        return switch (ratePlan) {
            case STANDARD, NON_REFUNDABLE -> calculateStandardPrice(room, checkIn, checkOut, ratePlan);
            case CORPORATE -> calculateCorporatePrice(room, checkIn, checkOut, companyCode);
            case WEEKEND_PACKAGE -> calculateWeekendPackagePrice(room, checkIn, checkOut);
            case LONG_STAY -> calculateLongStayPrice(room, checkIn, checkOut);
        };
    }

    private Price calculateStandardPrice(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = switch (ratePlan) {
            case STANDARD -> cents(BigDecimal.ZERO);
            case NON_REFUNDABLE -> cents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
            default -> cents(BigDecimal.ZERO);
        };
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private Price calculateCorporatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discountRate = CORPORATE_DISCOUNTS.get(companyCode);
        BigDecimal discount = cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private Price calculateWeekendPackagePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        String roomTypeStr = room.type().name();
        BigDecimal fixedPrice = WEEKEND_PACKAGE_PRICES.get(roomTypeStr);
        int nights = 2;
        return new Price(nights, fixedPrice, cents(BigDecimal.ZERO), fixedPrice);
    }

    private Price calculateLongStayPrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
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
