package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Factory and implementations for each rate plan's eligibility, pricing, and refund rules. */
final class RatePlanPolicies {

    private static final Map<String, BigDecimal> CORPORATE_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private static final Map<RoomType, BigDecimal> WEEKEND_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    static RatePlanPolicy forPlan(RatePlan plan, String companyCode) {
        return switch (plan) {
            case STANDARD -> new StandardPolicy();
            case NON_REFUNDABLE -> new NonRefundablePolicy();
            case CORPORATE -> new CorporatePolicy(companyCode);
            case WEEKEND_PACKAGE -> new WeekendPackagePolicy();
            case LONG_STAY -> new LongStayPolicy();
        };
    }

    private static final class StandardPolicy implements RatePlanPolicy {
        @Override
        public void validate(Room room, LocalDate checkIn, LocalDate checkOut) {}

        @Override
        public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
            return PriceCalculator.nightlySubtotal(room, checkIn, checkOut, BigDecimal.ZERO);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return amountPaid;
        }
    }

    private static final class NonRefundablePolicy implements RatePlanPolicy {
        private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

        @Override
        public void validate(Room room, LocalDate checkIn, LocalDate checkOut) {}

        @Override
        public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
            return PriceCalculator.nightlySubtotal(room, checkIn, checkOut, DISCOUNT_RATE);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    }

    private static final class CorporatePolicy implements RatePlanPolicy {
        private final BigDecimal discountRate;

        CorporatePolicy(String companyCode) {
            if (companyCode == null || !CORPORATE_DISCOUNTS.containsKey(companyCode)) {
                throw new HotelException(ErrorCode.INVALID_REQUEST,
                        "unknown company code; valid codes are TECNOSUL, AGROVALE, FARMAVIDA");
            }
            this.discountRate = CORPORATE_DISCOUNTS.get(companyCode);
        }

        @Override
        public void validate(Room room, LocalDate checkIn, LocalDate checkOut) {}

        @Override
        public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
            return PriceCalculator.nightlySubtotal(room, checkIn, checkOut, discountRate);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return amountPaid;
        }
    }

    private static final class WeekendPackagePolicy implements RatePlanPolicy {
        @Override
        public void validate(Room room, LocalDate checkIn, LocalDate checkOut) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY
                    || !checkOut.equals(checkIn.plusDays(2))) {
                throw new HotelException(ErrorCode.INVALID_REQUEST,
                        "WEEKEND_PACKAGE requires check-in on Friday and check-out on Sunday (exactly 2 nights)");
            }
        }

        @Override
        public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
            BigDecimal packagePrice = WEEKEND_PRICES.get(room.type());
            return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    }

    private static final class LongStayPolicy implements RatePlanPolicy {
        private static final int MIN_NIGHTS = 7;
        private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

        @Override
        public void validate(Room room, LocalDate checkIn, LocalDate checkOut) {
            if (ChronoUnit.DAYS.between(checkIn, checkOut) < MIN_NIGHTS) {
                throw new HotelException(ErrorCode.INVALID_REQUEST,
                        "LONG_STAY requires at least " + MIN_NIGHTS + " nights");
            }
        }

        @Override
        public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
            return PriceCalculator.nightlySubtotal(room, checkIn, checkOut, DISCOUNT_RATE);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return amountPaid;
        }
    }
}
