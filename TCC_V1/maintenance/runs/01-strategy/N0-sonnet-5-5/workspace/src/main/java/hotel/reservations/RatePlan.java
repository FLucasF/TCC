package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * A rate plan owns its three rules: who may book it ({@link #validate}), how the stay is priced
 * ({@link #adjust}) and what comes back on cancellation ({@link #refund}). A new plan is a new constant.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price adjust(Room room, StayDates stay, Price base) {
            return discounted(base, PriceCalculator.NON_REFUNDABLE_DISCOUNT);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },
    /** Agreement with a company: a discount that depends on the company code. Full refund. */
    CORPORATE {
        private final Map<String, BigDecimal> discounts = Map.of(
                "TECNOSUL", new BigDecimal("0.12"),
                "AGROVALE", new BigDecimal("0.18"),
                "FARMAVIDA", new BigDecimal("0.25"));

        @Override
        void validate(StayDates stay) {
            if (stay.companyCode() == null || !discounts.containsKey(stay.companyCode())) {
                throw invalid("the corporate rate plan needs a valid companyCode");
            }
        }

        @Override
        Price adjust(Room room, StayDates stay, Price base) {
            return discounted(base, discounts.get(stay.companyCode()));
        }
    },
    /** Friday to Sunday, two nights, a closed price all year round. Half comes back on cancellation. */
    WEEKEND_PACKAGE {
        private final Map<RoomType, BigDecimal> prices = Map.of(
                RoomType.STANDARD, new BigDecimal("520.00"),
                RoomType.DELUXE, new BigDecimal("820.00"),
                RoomType.SUITE, new BigDecimal("1200.00"));

        @Override
        void validate(StayDates stay) {
            if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                    || !stay.checkOut().equals(stay.checkIn().plusDays(2))) {
                throw invalid("the weekend package is only for two nights, from Friday to Sunday");
            }
        }

        @Override
        Price adjust(Room room, StayDates stay, Price base) {
            BigDecimal price = PriceCalculator.cents(prices.get(room.type()));
            return new Price(base.nights(), price, PriceCalculator.cents(BigDecimal.ZERO), price);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },
    /** 7 nights or more: 10% off, high season still applies. Full refund. */
    LONG_STAY {
        @Override
        void validate(StayDates stay) {
            if (stay.nights() < 7) {
                throw invalid("the long stay rate plan needs at least 7 nights");
            }
        }

        @Override
        Price adjust(Room room, StayDates stay, Price base) {
            return discounted(base, new BigDecimal("0.10"));
        }
    };

    /** The dates and company code of the stay being booked. */
    record StayDates(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        long nights() {
            return ChronoUnit.DAYS.between(checkIn, checkOut);
        }
    }

    /** Throws INVALID_REQUEST when this plan cannot be used for the stay. */
    void validate(StayDates stay) {
    }

    /** Turns the plain price (nights at the room's rate, high season included) into this plan's price. */
    Price adjust(Room room, StayDates stay, Price base) {
        return base;
    }

    /** What goes back to the guest, given what was paid. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static Price discounted(Price base, BigDecimal rate) {
        BigDecimal discount = PriceCalculator.cents(base.subtotal().multiply(rate));
        return new Price(base.nights(), base.subtotal(), discount, base.subtotal().subtract(discount));
    }

    private static HotelException invalid(String message) {
        return new HotelException(ErrorCode.INVALID_REQUEST, message);
    }
}
