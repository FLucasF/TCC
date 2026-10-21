package hotel.reservations;

import static hotel.reservations.PriceCalculator.cents;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * A rate plan: who may use it, how the stay is priced from the seasonal price of its nights, and how much
 * comes back on cancellation. A new plan is a new constant that overrides only what is its own.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        BigDecimal discountRate(NewReservation stay) {
            return new BigDecimal("0.15");
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(BigDecimal.ZERO);
        }
    },
    /** Companies with an agreement get their own discount; the guest gives the company code. */
    CORPORATE {
        private final Map<String, BigDecimal> discounts = Map.of(
                "TECNOSUL", new BigDecimal("0.12"),
                "AGROVALE", new BigDecimal("0.18"),
                "FARMAVIDA", new BigDecimal("0.25"));

        @Override
        void validate(NewReservation stay) {
            if (stay.companyCode() == null || !discounts.containsKey(stay.companyCode())) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "a known company code is required");
            }
        }

        @Override
        BigDecimal discountRate(NewReservation stay) {
            return discounts.get(stay.companyCode());
        }
    },
    /** Friday to Sunday, two nights, at a closed price per room type all year round; half comes back. */
    WEEKEND_PACKAGE {
        private final Map<RoomType, BigDecimal> prices = Map.of(
                RoomType.STANDARD, new BigDecimal("520.00"),
                RoomType.DELUXE, new BigDecimal("820.00"),
                RoomType.SUITE, new BigDecimal("1200.00"));

        @Override
        void validate(NewReservation stay) {
            if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY
                    || !stay.checkOut().equals(stay.checkIn().plusDays(2))) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package is from a Friday to the following Sunday");
            }
        }

        @Override
        Price price(NewReservation stay, Room room, Price seasonal) {
            BigDecimal packagePrice = cents(prices.get(room.type()));
            return new Price(seasonal.nights(), packagePrice, cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(amountPaid.multiply(new BigDecimal("0.50")));
        }
    },
    /** 10% off for 7 nights or more. */
    LONG_STAY {
        @Override
        void validate(NewReservation stay) {
            if (ChronoUnit.DAYS.between(stay.checkIn(), stay.checkOut()) < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the long stay plan needs 7 nights or more");
            }
        }

        @Override
        BigDecimal discountRate(NewReservation stay) {
            return new BigDecimal("0.10");
        }
    };

    /** Refuses, with INVALID_REQUEST, a stay this plan cannot be used for. */
    void validate(NewReservation stay) {
    }

    /** The fraction taken off the sum of the nights. */
    BigDecimal discountRate(NewReservation stay) {
        return BigDecimal.ZERO;
    }

    /** The price of the stay, given the nights at their seasonal price (no discount yet). */
    Price price(NewReservation stay, Room room, Price seasonal) {
        BigDecimal discount = cents(seasonal.subtotal().multiply(discountRate(stay)));
        return new Price(seasonal.nights(), seasonal.subtotal(), discount, seasonal.subtotal().subtract(discount));
    }

    /** What comes back when a reservation that had paid this amount is cancelled. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
