package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * How a plan prices a stay and how much comes back on cancellation. Each plan brings its own rules, so
 * every new plan is a new constant with its own price and refund methods — the rest of the system
 * does not change.
 */
public enum RatePlan {

    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calc) {
            return discounted(calc.nightlySubtotal(room, checkIn, checkOut), BigDecimal.ZERO);
        }

        @Override
        public BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** 15% off; nothing comes back on cancellation. */
    NON_REFUNDABLE {
        private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

        @Override
        public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calc) {
            return discounted(calc.nightlySubtotal(room, checkIn, checkOut), DISCOUNT_RATE);
        }

        @Override
        public BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /** Discount by the company agreed in advance; everything comes back on cancellation. */
    CORPORATE {
        private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
                "TECNOSUL", new BigDecimal("0.12"),
                "AGROVALE", new BigDecimal("0.18"),
                "FARMAVIDA", new BigDecimal("0.25"));

        @Override
        public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calc) {
            BigDecimal discountRate = companyCode == null ? null : COMPANY_DISCOUNTS.get(companyCode);
            if (discountRate == null) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the corporate plan requires a known company code");
            }
            return discounted(calc.nightlySubtotal(room, checkIn, checkOut), discountRate);
        }

        @Override
        public BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** Fixed Friday-to-Sunday package, flat price per room type; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        private static final Map<RoomType, BigDecimal> PACKAGE_PRICE = Map.of(
                RoomType.STANDARD, new BigDecimal("520.00"),
                RoomType.DELUXE, new BigDecimal("820.00"),
                RoomType.SUITE, new BigDecimal("1200.00"));
        private static final BigDecimal HALF = new BigDecimal("0.50");

        @Override
        public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calc) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY
                    || checkOut.getDayOfWeek() != DayOfWeek.SUNDAY
                    || ChronoUnit.DAYS.between(checkIn, checkOut) != 2) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the weekend package goes from Friday to the following Sunday");
            }
            BigDecimal total = PriceCalculator.cents(PACKAGE_PRICE.get(room.type()));
            return new Price(2, total, PriceCalculator.cents(BigDecimal.ZERO), total);
        }

        @Override
        public BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(HALF));
        }
    },

    /** 10% off for stays of 7 nights or more; everything comes back on cancellation. */
    LONG_STAY {
        private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
        private static final int MINIMUM_NIGHTS = 7;

        @Override
        public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calc) {
            if (ChronoUnit.DAYS.between(checkIn, checkOut) < MINIMUM_NIGHTS) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the long stay plan requires at least " + MINIMUM_NIGHTS + " nights");
            }
            return discounted(calc.nightlySubtotal(room, checkIn, checkOut), DISCOUNT_RATE);
        }

        @Override
        public BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    public abstract Price price(
            Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calc);

    public abstract BigDecimal refund(BigDecimal amountPaid);

    static Price discounted(PriceCalculator.NightlySubtotal nightly, BigDecimal discountRate) {
        BigDecimal discount = PriceCalculator.cents(nightly.subtotal().multiply(discountRate));
        return new Price(nightly.nights(), nightly.subtotal(), discount, nightly.subtotal().subtract(discount));
    }
}
