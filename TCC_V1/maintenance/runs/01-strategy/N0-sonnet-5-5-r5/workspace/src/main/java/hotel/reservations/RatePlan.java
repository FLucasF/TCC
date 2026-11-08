package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/**
 * A rate plan owns its three rules: who may use it ({@link #validate}), how a stay is priced
 * ({@link #price}) and what comes back on cancellation ({@link #refund}). A new plan is a new constant
 * here; nothing else has to learn about it.
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(Room room, int nights, BigDecimal subtotal, String companyCode) {
            return discounted(nights, subtotal, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /** Company agreement: the company's discount on the nights; the company code is required. */
    CORPORATE {
        private final Map<String, BigDecimal> discounts = Map.of(
                "TECNOSUL", new BigDecimal("0.12"),
                "AGROVALE", new BigDecimal("0.18"),
                "FARMAVIDA", new BigDecimal("0.25"));

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (companyCode == null || !discounts.containsKey(companyCode)) {
                throw invalid("the corporate rate needs the code of a company with an agreement");
            }
        }

        @Override
        Price price(Room room, int nights, BigDecimal subtotal, String companyCode) {
            return discounted(nights, subtotal, discounts.get(companyCode));
        }
    },

    /** Friday to the following Sunday, two nights, at a closed price all year; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        private final Map<RoomType, BigDecimal> prices = Map.of(
                RoomType.STANDARD, new BigDecimal("520.00"),
                RoomType.DELUXE, new BigDecimal("820.00"),
                RoomType.SUITE, new BigDecimal("1200.00"));

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || !checkOut.equals(checkIn.plusDays(2))) {
                throw invalid("the weekend package is from a Friday to the Sunday right after it");
            }
        }

        @Override
        Price price(Room room, int nights, BigDecimal subtotal, String companyCode) {
            BigDecimal closedPrice = PriceCalculator.cents(prices.get(room.type()));
            return new Price(nights, closedPrice, PriceCalculator.cents(BigDecimal.ZERO), closedPrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },

    /** 10% off the nights, for stays of 7 nights or more. */
    LONG_STAY {
        static final int MINIMUM_NIGHTS = 7;

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (checkIn.plusDays(MINIMUM_NIGHTS).isAfter(checkOut)) {
                throw invalid("the long-stay rate needs at least " + MINIMUM_NIGHTS + " nights");
            }
        }

        @Override
        Price price(Room room, int nights, BigDecimal subtotal, String companyCode) {
            return discounted(nights, subtotal, new BigDecimal("0.10"));
        }
    };

    /** Throws INVALID_REQUEST when this plan cannot be used for the stay. */
    void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
    }

    /** The price of the stay, given the sum of its nights at the room's rates. */
    Price price(Room room, int nights, BigDecimal subtotal, String companyCode) {
        return discounted(nights, subtotal, BigDecimal.ZERO);
    }

    /** What goes back to the guest who cancels after paying {@code amountPaid}. */
    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    static Price discounted(int nights, BigDecimal subtotal, BigDecimal rate) {
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(rate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    static HotelException invalid(String message) {
        return new HotelException(ErrorCode.INVALID_REQUEST, message);
    }
}
