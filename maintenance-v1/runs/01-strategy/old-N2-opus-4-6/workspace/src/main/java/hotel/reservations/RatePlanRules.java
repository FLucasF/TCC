package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

sealed interface RatePlanRules {

    void validate(LocalDate checkIn, LocalDate checkOut);

    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateRefund(BigDecimal amountPaid);

    static RatePlanRules of(RatePlan plan, String companyCode) {
        return switch (plan) {
            case STANDARD -> new Standard();
            case NON_REFUNDABLE -> new NonRefundable();
            case CORPORATE -> new Corporate(companyCode);
            case WEEKEND_PACKAGE -> new WeekendPackage();
            case LONG_STAY -> new LongStay();
        };
    }

    final class Standard implements RatePlanRules {
        @Override
        public void validate(LocalDate checkIn, LocalDate checkOut) {}

        @Override
        public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
            return PriceCalculator.nightlyPrice(room, checkIn, checkOut, BigDecimal.ZERO);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return amountPaid;
        }
    }

    final class NonRefundable implements RatePlanRules {
        static final BigDecimal DISCOUNT = new BigDecimal("0.15");

        @Override
        public void validate(LocalDate checkIn, LocalDate checkOut) {}

        @Override
        public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
            return PriceCalculator.nightlyPrice(room, checkIn, checkOut, DISCOUNT);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    }

    final class Corporate implements RatePlanRules {
        private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
                "TECNOSUL", new BigDecimal("0.12"),
                "AGROVALE", new BigDecimal("0.18"),
                "FARMAVIDA", new BigDecimal("0.25"));

        private final String companyCode;

        Corporate(String companyCode) {
            this.companyCode = companyCode;
        }

        @Override
        public void validate(LocalDate checkIn, LocalDate checkOut) {
            if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
                throw new HotelException(ErrorCode.INVALID_REQUEST,
                        "the corporate plan requires a valid company code");
            }
        }

        @Override
        public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
            return PriceCalculator.nightlyPrice(room, checkIn, checkOut, COMPANY_DISCOUNTS.get(companyCode));
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return amountPaid;
        }
    }

    final class WeekendPackage implements RatePlanRules {
        private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
                RoomType.STANDARD, new BigDecimal("520.00"),
                RoomType.DELUXE, new BigDecimal("820.00"),
                RoomType.SUITE, new BigDecimal("1200.00"));

        @Override
        public void validate(LocalDate checkIn, LocalDate checkOut) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY
                    || !checkOut.equals(checkIn.plusDays(2))) {
                throw new HotelException(ErrorCode.INVALID_REQUEST,
                        "the weekend package requires check-in on Friday and check-out on Sunday");
            }
        }

        @Override
        public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
            BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
            return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.50")));
        }
    }

    final class LongStay implements RatePlanRules {
        static final BigDecimal DISCOUNT = new BigDecimal("0.10");
        static final int MINIMUM_NIGHTS = 7;

        @Override
        public void validate(LocalDate checkIn, LocalDate checkOut) {
            long nights = checkIn.until(checkOut).getDays();
            if (nights < MINIMUM_NIGHTS) {
                throw new HotelException(ErrorCode.INVALID_REQUEST,
                        "the long stay plan requires at least " + MINIMUM_NIGHTS + " nights");
            }
        }

        @Override
        public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
            return PriceCalculator.nightlyPrice(room, checkIn, checkOut, DISCOUNT);
        }

        @Override
        public BigDecimal calculateRefund(BigDecimal amountPaid) {
            return amountPaid;
        }
    }
}
