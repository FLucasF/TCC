package hotel.reservations;

import static hotel.reservations.PriceCalculator.cents;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * A way of selling a stay. Each plan brings its own rules: who may use it ({@link #validate}), how the
 * stay is priced ({@link #price}) and what comes back when a reservation is cancelled ({@link #refund}).
 * A new plan is a new constant here (and a value in the database column, which is plain text).
 */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return discounted(calculator, room, checkIn, checkOut, BigDecimal.ZERO);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return discounted(calculator, room, checkIn, checkOut, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(BigDecimal.ZERO);
        }
    },
    /** Agreement with a company: its discount on the nights, identified by the company code. Full refund. */
    CORPORATE {
        private final Map<String, BigDecimal> agreements = Map.of(
                "TECNOSUL", new BigDecimal("0.12"),
                "AGROVALE", new BigDecimal("0.18"),
                "FARMAVIDA", new BigDecimal("0.25"));

        @Override
        void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (companyCode == null || !agreements.containsKey(companyCode)) {
                throw invalid("the corporate rate needs the code of a company with an agreement");
            }
        }

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return discounted(calculator, room, checkIn, checkOut, agreements.get(companyCode));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },
    /** Friday to Sunday (two nights) for a fixed price all year round, with no discount. Half comes back. */
    WEEKEND_PACKAGE {
        private final Map<RoomType, BigDecimal> prices = Map.of(
                RoomType.STANDARD, new BigDecimal("520.00"),
                RoomType.DELUXE, new BigDecimal("820.00"),
                RoomType.SUITE, new BigDecimal("1200.00"));

        @Override
        void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || !checkOut.equals(checkIn.plusDays(2))) {
                throw invalid("the weekend package is from a Friday to the Sunday right after it");
            }
        }

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            BigDecimal packagePrice = cents(prices.get(room.type()));
            return new Price(2, packagePrice, cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },
    /** For stays of 7 nights or more: 10% off the nights (high season still applies). Full refund. */
    LONG_STAY {
        @Override
        void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (ChronoUnit.DAYS.between(checkIn, checkOut) < 7) {
                throw invalid("the long-stay rate needs at least 7 nights");
            }
        }

        @Override
        Price price(PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return discounted(calculator, room, checkIn, checkOut, new BigDecimal("0.10"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    /** Throws INVALID_REQUEST if this plan cannot be used for the stay. Most plans have no restriction. */
    void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    }

    /** The price of the stay under this plan (the stay has already been validated). */
    abstract Price price(
            PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** What goes back to the guest who cancels after paying {@code amountPaid}. */
    abstract BigDecimal refund(BigDecimal amountPaid);

    /** The nights at the usual prices, minus a percentage of their sum. */
    static Price discounted(
            PriceCalculator calculator, Room room, LocalDate checkIn, LocalDate checkOut, BigDecimal percentage) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal subtotal = calculator.sumOfNights(room, checkIn, checkOut);
        BigDecimal discount = cents(subtotal.multiply(percentage));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private static HotelException invalid(String message) {
        return new HotelException(ErrorCode.INVALID_REQUEST, message);
    }
}
