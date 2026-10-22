package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.UnaryOperator;

/** The policies of the rate plans. */
final class RatePlanPolicies {

    private static final UnaryOperator<BigDecimal> FULL_REFUND = amountPaid -> amountPaid;
    private static final UnaryOperator<BigDecimal> NO_REFUND = amountPaid -> PriceCalculator.cents(BigDecimal.ZERO);

    static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    static final Map<RoomType, BigDecimal> WEEKEND_PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    static final int LONG_STAY_MIN_NIGHTS = 7;

    private RatePlanPolicies() {
    }

    /** The nightly prices, minus a percentage of their sum. */
    private static class Discounted implements RatePlanPolicy {
        private final BigDecimal percentage;
        private final UnaryOperator<BigDecimal> refund;

        Discounted(BigDecimal percentage, UnaryOperator<BigDecimal> refund) {
            this.percentage = percentage;
            this.refund = refund;
        }

        @Override
        public Price price(PriceCalculator calculator, Room room, NewReservation request) {
            return calculator.discounted(room, request.checkIn(), request.checkOut(), percentage);
        }

        @Override
        public BigDecimal refund(BigDecimal amountPaid) {
            return refund.apply(amountPaid);
        }
    }

    static RatePlanPolicy standard() {
        return new Discounted(BigDecimal.ZERO, FULL_REFUND);
    }

    static RatePlanPolicy nonRefundable() {
        return new Discounted(new BigDecimal("0.15"), NO_REFUND);
    }

    static RatePlanPolicy corporate() {
        return new RatePlanPolicy() {
            @Override
            public Price price(PriceCalculator calculator, Room room, NewReservation request) {
                String code = request.companyCode();
                BigDecimal percentage = code == null ? null : COMPANY_DISCOUNTS.get(code.trim());
                if (percentage == null) {
                    throw new HotelException(
                            ErrorCode.INVALID_REQUEST, "the corporate plan needs the code of a partner company");
                }
                return calculator.discounted(room, request.checkIn(), request.checkOut(), percentage);
            }

            @Override
            public BigDecimal refund(BigDecimal amountPaid) {
                return FULL_REFUND.apply(amountPaid);
            }
        };
    }

    static RatePlanPolicy weekendPackage() {
        return new RatePlanPolicy() {
            @Override
            public Price price(PriceCalculator calculator, Room room, NewReservation request) {
                boolean friday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY;
                if (!friday || !request.checkOut().equals(request.checkIn().plusDays(2))) {
                    throw new HotelException(
                            ErrorCode.INVALID_REQUEST,
                            "the weekend package is from a Friday to the following Sunday");
                }
                BigDecimal packagePrice = PriceCalculator.cents(WEEKEND_PACKAGE_PRICES.get(room.type()));
                return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
            }

            @Override
            public BigDecimal refund(BigDecimal amountPaid) {
                return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
            }
        };
    }

    static RatePlanPolicy longStay() {
        return new Discounted(new BigDecimal("0.10"), FULL_REFUND) {
            @Override
            public Price price(PriceCalculator calculator, Room room, NewReservation request) {
                long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
                if (nights < LONG_STAY_MIN_NIGHTS) {
                    throw new HotelException(
                            ErrorCode.INVALID_REQUEST,
                            "the long-stay plan needs at least " + LONG_STAY_MIN_NIGHTS + " nights");
                }
                return super.price(calculator, room, request);
            }
        };
    }
}
