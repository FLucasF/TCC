package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/**
 * A pricing plan: how it prices a stay, when a stay may use it, and what the guest gets back when
 * cancelling. Each constant knows its own rules, so a new plan is a new constant, not a new branch
 * spread across the code.
 */
public enum RatePlan {

    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        }

        @Override
        Price price(Room room, int nights, BigDecimal nightlySubtotal, String companyCode) {
            return undiscountedPrice(nights, nightlySubtotal);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        }

        @Override
        Price price(Room room, int nights, BigDecimal nightlySubtotal, String companyCode) {
            return discountedPrice(nights, nightlySubtotal, NON_REFUNDABLE_DISCOUNT);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /** A discount agreed with a partner company; the guest gives its code when booking. */
    CORPORATE {
        @Override
        void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
            if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the CORPORATE plan needs a valid company code");
            }
        }

        @Override
        Price price(Room room, int nights, BigDecimal nightlySubtotal, String companyCode) {
            return discountedPrice(nights, nightlySubtotal, COMPANY_DISCOUNTS.get(companyCode));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** Friday to Sunday, exactly two nights, for a closed price that holds the whole year. */
    WEEKEND_PACKAGE {
        @Override
        void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || nights != 2) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the WEEKEND_PACKAGE plan only books Friday to Sunday, two nights");
            }
        }

        @Override
        Price price(Room room, int nights, BigDecimal nightlySubtotal, String companyCode) {
            BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
            return new Price(nights, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(WEEKEND_PACKAGE_REFUND_FRACTION));
        }
    },

    /** 7 nights or more, 10% off; high-season nights still carry their surcharge. */
    LONG_STAY {
        @Override
        void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
            if (nights < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the LONG_STAY plan needs at least 7 nights");
            }
        }

        @Override
        Price price(Room room, int nights, BigDecimal nightlySubtotal, String companyCode) {
            return discountedPrice(nights, nightlySubtotal, LONG_STAY_DISCOUNT);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");
    private static final BigDecimal LONG_STAY_DISCOUNT = new BigDecimal("0.10");
    private static final BigDecimal WEEKEND_PACKAGE_REFUND_FRACTION = new BigDecimal("0.5");

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    /** Rejects a stay this plan cannot book; a plan ignores the fields it does not care about. */
    abstract void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode);

    /** The price of the stay, given what the nights would cost with no plan applied. */
    abstract Price price(Room room, int nights, BigDecimal nightlySubtotal, String companyCode);

    /** What the guest gets back when cancelling a reservation that had been paid. */
    abstract BigDecimal refund(BigDecimal amountPaid);

    private static Price undiscountedPrice(int nights, BigDecimal subtotal) {
        return new Price(nights, subtotal, PriceCalculator.cents(BigDecimal.ZERO), subtotal);
    }

    private static Price discountedPrice(int nights, BigDecimal subtotal, BigDecimal rate) {
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(rate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }
}
