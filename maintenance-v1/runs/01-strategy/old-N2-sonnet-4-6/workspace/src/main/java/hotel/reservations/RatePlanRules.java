package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static hotel.reservations.PriceCalculator.cents;

/**
 * All rate-plan-specific rules in one place: who may book, how the price is built, and what comes
 * back on cancellation. Adding a new plan touches only this class and the RatePlan enum.
 */
public final class RatePlanRules {

    static final Map<String, BigDecimal> CORPORATE_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    static final Map<RoomType, BigDecimal> WEEKEND_PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    private RatePlanRules() {}

    /** Throws INVALID_REQUEST if the reservation cannot be made under the given plan. */
    public static void validate(
            RatePlan ratePlan, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        switch (ratePlan) {
            case CORPORATE -> {
                if (companyCode == null || !CORPORATE_DISCOUNTS.containsKey(companyCode)) {
                    throw new HotelException(
                            ErrorCode.INVALID_REQUEST,
                            "a valid company code is required for the CORPORATE rate plan");
                }
            }
            case WEEKEND_PACKAGE -> {
                boolean startsOnFriday = checkIn.getDayOfWeek() == DayOfWeek.FRIDAY;
                boolean exactlyTwoNights = ChronoUnit.DAYS.between(checkIn, checkOut) == 2;
                if (!startsOnFriday || !exactlyTwoNights) {
                    throw new HotelException(
                            ErrorCode.INVALID_REQUEST,
                            "WEEKEND_PACKAGE requires check-in on Friday and check-out on Sunday (2 nights)");
                }
            }
            case LONG_STAY -> {
                long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
                if (nights < 7) {
                    throw new HotelException(
                            ErrorCode.INVALID_REQUEST,
                            "LONG_STAY requires at least 7 nights");
                }
            }
            default -> {}
        }
    }

    /**
     * Fixed price for WEEKEND_PACKAGE, or null for plans that use the normal per-night subtotal.
     * When non-null, PriceCalculator skips the per-night loop and uses this price directly.
     */
    public static BigDecimal weekendPackagePrice(RoomType roomType) {
        return WEEKEND_PACKAGE_PRICES.get(roomType);
    }

    /** Discount amount to subtract from the subtotal; zero for plans without a discount. */
    public static BigDecimal discount(RatePlan ratePlan, BigDecimal subtotal, String companyCode) {
        return switch (ratePlan) {
            case STANDARD, WEEKEND_PACKAGE -> cents(BigDecimal.ZERO);
            case NON_REFUNDABLE -> cents(subtotal.multiply(new BigDecimal("0.15")));
            case CORPORATE -> cents(subtotal.multiply(CORPORATE_DISCOUNTS.get(companyCode)));
            case LONG_STAY -> cents(subtotal.multiply(new BigDecimal("0.10")));
        };
    }

    /** Refund amount for a paid reservation being cancelled. */
    public static BigDecimal refund(RatePlan ratePlan, BigDecimal amountPaid) {
        return switch (ratePlan) {
            case STANDARD, CORPORATE, LONG_STAY -> amountPaid;
            case NON_REFUNDABLE -> cents(BigDecimal.ZERO);
            case WEEKEND_PACKAGE -> cents(amountPaid.divide(new BigDecimal("2")));
        };
    }
}
