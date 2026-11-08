package hotel.reservations;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * A rate plan owns its three rules: who may use it ({@link #validate}), how a stay is priced
 * ({@link #price}) and what comes back on cancellation ({@link #refund}). A new plan is a new constant
 * here; nothing else has to change.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        BigDecimal discount(StayTerms stay, BigDecimal subtotal) {
            return percentOff(subtotal, "0.15");
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /** Company agreement: the discount depends on the company code; full refund. */
    CORPORATE {
        private static final Map<String, String> DISCOUNTS =
                Map.of("TECNOSUL", "0.12", "AGROVALE", "0.18", "FARMAVIDA", "0.25");

        @Override
        void validate(StayTerms stay) {
            if (stay.companyCode() == null || !DISCOUNTS.containsKey(stay.companyCode())) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the corporate rate needs the code of a company with an agreement");
            }
        }

        @Override
        BigDecimal discount(StayTerms stay, BigDecimal subtotal) {
            return percentOff(subtotal, DISCOUNTS.get(stay.companyCode()));
        }
    },

    /** Friday to Sunday, two nights, at a fixed price per room type all year; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        private static final Map<RoomType, String> PRICES =
                Map.of(RoomType.STANDARD, "520.00", RoomType.DELUXE, "820.00", RoomType.SUITE, "1200.00");

        @Override
        void validate(StayTerms stay) {
            if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || stay.nights() != 2) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package is from a Friday to the following Sunday");
            }
        }

        @Override
        Price price(StayTerms stay, PriceCalculator calculator) {
            BigDecimal packagePrice = PriceCalculator.cents(new BigDecimal(PRICES.get(stay.room().type())));
            return new Price(stay.nights(), packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return percentOff(amountPaid, "0.50");
        }
    },

    /** 7 nights or more: 10% off; full refund. */
    LONG_STAY {
        @Override
        void validate(StayTerms stay) {
            if (stay.nights() < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the long-stay rate needs at least 7 nights");
            }
        }

        @Override
        BigDecimal discount(StayTerms stay, BigDecimal subtotal) {
            return percentOff(subtotal, "0.10");
        }
    };

    /** Throws INVALID_REQUEST when this plan cannot be used for the stay. */
    void validate(StayTerms stay) {
    }

    /** The price of the stay; by default the nights (with high season) minus {@link #discount}. */
    Price price(StayTerms stay, PriceCalculator calculator) {
        BigDecimal subtotal = calculator.nightsTotal(stay.room(), stay.checkIn(), stay.checkOut());
        BigDecimal discount = discount(stay, subtotal);
        return new Price(stay.nights(), subtotal, discount, subtotal.subtract(discount));
    }

    /** The discount on the sum of the nights; none by default. */
    BigDecimal discount(StayTerms stay, BigDecimal subtotal) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }

    /** What goes back to the guest who cancels after paying {@code amountPaid}; all of it by default. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static BigDecimal percentOff(BigDecimal amount, String rate) {
        return PriceCalculator.cents(amount.multiply(new BigDecimal(rate)));
    }
}
