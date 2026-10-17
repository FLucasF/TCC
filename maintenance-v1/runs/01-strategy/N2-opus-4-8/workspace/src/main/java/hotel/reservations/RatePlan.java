package hotel.reservations;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * A rate plan: its own way of pricing a stay, its own rule on who may use it, and its own rule on
 * what comes back when a paid reservation is cancelled. The commercial team keeps inventing plans, so
 * each one is a constant here that answers those three questions; a new plan is a new constant and
 * touches nothing else.
 */
public enum RatePlan {

    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        Price price(PriceCalculator calculator, RateRequest request) {
            return percentageOff(calculator, request, BigDecimal.ZERO);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(PriceCalculator calculator, RateRequest request) {
            return percentageOff(calculator, request, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /** A company rate: the company code sets the discount. Everything comes back on cancellation. */
    CORPORATE {
        @Override
        void checkEligible(RateRequest request) {
            if (companyDiscount(request.companyCode()) == null) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the corporate rate needs a known company code");
            }
        }

        @Override
        Price price(PriceCalculator calculator, RateRequest request) {
            return percentageOff(calculator, request, companyDiscount(request.companyCode()));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** Friday to the Sunday after, two nights, at a flat price all year. Half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        void checkEligible(RateRequest request) {
            if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || nights(request) != 2) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package runs from a Friday to the Sunday after");
            }
        }

        @Override
        Price price(PriceCalculator calculator, RateRequest request) {
            // Flat price, so the nightly subtotal and the total are the package price, with no discount.
            BigDecimal flatPrice = PriceCalculator.cents(WEEKEND_PRICES.get(request.room().type()));
            return new Price(nights(request), flatPrice, PriceCalculator.cents(BigDecimal.ZERO), flatPrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },

    /** Seven nights or more get 10% off. Everything comes back on cancellation. */
    LONG_STAY {
        @Override
        void checkEligible(RateRequest request) {
            if (nights(request) < 7) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the long-stay rate needs at least 7 nights");
            }
        }

        @Override
        Price price(PriceCalculator calculator, RateRequest request) {
            return percentageOff(calculator, request, new BigDecimal("0.10"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private static final Map<RoomType, BigDecimal> WEEKEND_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    /** Whether this stay may use the plan. Most plans accept any stay; the demanding ones override. */
    void checkEligible(RateRequest request) {
    }

    /** The price the guest pays on this plan. */
    abstract Price price(PriceCalculator calculator, RateRequest request);

    /** What comes back when a paid reservation on this plan is cancelled. */
    abstract BigDecimal refund(BigDecimal amountPaid);

    /** The nightly subtotal with a flat percentage off: the shape most plans share. */
    static Price percentageOff(PriceCalculator calculator, RateRequest request, BigDecimal rate) {
        Price base = calculator.base(request.room(), request.checkIn(), request.checkOut());
        BigDecimal discount = PriceCalculator.cents(base.subtotal().multiply(rate));
        return new Price(base.nights(), base.subtotal(), discount, base.subtotal().subtract(discount));
    }

    static int nights(RateRequest request) {
        return (int) ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
    }

    static BigDecimal companyDiscount(String companyCode) {
        return companyCode == null ? null : COMPANY_DISCOUNTS.get(companyCode);
    }
}
