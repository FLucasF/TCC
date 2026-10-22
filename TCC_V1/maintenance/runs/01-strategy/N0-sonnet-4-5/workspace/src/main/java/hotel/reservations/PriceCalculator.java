package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
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
            "FARMAVIDA", new BigDecimal("0.25"));

    static final Map<RoomType, BigDecimal> WEEKEND_PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }

        BigDecimal discount;
        BigDecimal total;

        switch (ratePlan) {
            case STANDARD:
                discount = cents(BigDecimal.ZERO);
                total = subtotal;
                break;
            case NON_REFUNDABLE:
                discount = cents(subtotal.multiply(NON_REFUNDABLE_DISCOUNT));
                total = subtotal.subtract(discount);
                break;
            case CORPORATE:
                if (companyCode == null || !CORPORATE_DISCOUNTS.containsKey(companyCode)) {
                    throw new HotelException(ErrorCode.INVALID_REQUEST,
                            "corporate rate plan requires a valid company code");
                }
                BigDecimal corporateDiscount = CORPORATE_DISCOUNTS.get(companyCode);
                discount = cents(subtotal.multiply(corporateDiscount));
                total = subtotal.subtract(discount);
                break;
            case WEEKEND_PACKAGE:
                if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY
                        || checkOut.getDayOfWeek() != DayOfWeek.SUNDAY
                        || nights != 2) {
                    throw new HotelException(ErrorCode.INVALID_REQUEST,
                            "weekend package requires check-in on Friday and check-out on Sunday (2 nights)");
                }
                BigDecimal packagePrice = WEEKEND_PACKAGE_PRICES.get(room.type());
                subtotal = packagePrice;
                discount = cents(BigDecimal.ZERO);
                total = packagePrice;
                break;
            case LONG_STAY:
                if (nights < 7) {
                    throw new HotelException(ErrorCode.INVALID_REQUEST,
                            "long stay rate plan requires at least 7 nights");
                }
                discount = cents(subtotal.multiply(LONG_STAY_DISCOUNT));
                total = subtotal.subtract(discount);
                break;
            default:
                discount = cents(BigDecimal.ZERO);
                total = subtotal;
        }

        return new Price(nights, subtotal, discount, total);
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
