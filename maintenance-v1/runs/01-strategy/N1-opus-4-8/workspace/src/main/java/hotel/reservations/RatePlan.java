package hotel.reservations;

import static hotel.reservations.PriceCalculator.cents;

import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * The rate plans the inn sells. Each one knows the three things that set it apart: how to price a
 * stay, who may use it (a stay the plan refuses cannot be booked), and how much comes back when a
 * confirmed reservation is cancelled. A new plan is a new constant here, with those three answers;
 * nothing else needs to change.
 */
public enum RatePlan {

    /** Full price; everything comes back on cancellation. */
    STANDARD {
        @Override
        Price price(PricingContext context) {
            return nightlyPrice(context, BigDecimal.ZERO);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** 15% off the nights, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(PricingContext context) {
            return nightlyPrice(context, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(BigDecimal.ZERO);
        }
    },

    /** A company deal: the discount depends on the company code, which the guest must supply. */
    CORPORATE {
        @Override
        void validate(PricingContext context) {
            companyDiscount(context.companyCode());
        }

        @Override
        Price price(PricingContext context) {
            return nightlyPrice(context, companyDiscount(context.companyCode()));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** Friday to the next Sunday, two nights, at a flat price all year; half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        void validate(PricingContext context) {
            boolean fridayToSunday = context.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                    && context.checkOut().equals(context.checkIn().plusDays(2));
            if (!fridayToSunday) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the weekend package is Friday to the next Sunday, exactly two nights");
            }
        }

        @Override
        Price price(PricingContext context) {
            BigDecimal packagePrice = cents(WEEKEND_PACKAGE_PRICES.get(context.room().type()));
            return new Price(2, packagePrice, cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },

    /** Seven nights or more: 10% off the nights (high season still counts); everything comes back. */
    LONG_STAY {
        @Override
        void validate(PricingContext context) {
            if (PriceCalculator.nights(context.checkIn(), context.checkOut()) < LONG_STAY_MIN_NIGHTS) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the long-stay plan needs at least " + LONG_STAY_MIN_NIGHTS + " nights");
            }
        }

        @Override
        Price price(PricingContext context) {
            return nightlyPrice(context, new BigDecimal("0.10"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    private static final Map<String, BigDecimal> CORPORATE_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private static final Map<RoomType, BigDecimal> WEEKEND_PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    private static final int LONG_STAY_MIN_NIGHTS = 7;

    /** Whether this plan accepts the stay. The default plan takes any stay; others may refuse. */
    void validate(PricingContext context) {}

    /** The price of the stay under this plan. */
    abstract Price price(PricingContext context);

    /** What comes back when a confirmed reservation on this plan is cancelled. */
    abstract BigDecimal refund(BigDecimal amountPaid);

    /** The nights at the room's rate (high season included), with a flat discount on the sum. */
    private static Price nightlyPrice(PricingContext context, BigDecimal discountRate) {
        int nights = PriceCalculator.nights(context.checkIn(), context.checkOut());
        BigDecimal subtotal = PriceCalculator.nightlySubtotal(context.room(), context.checkIn(), context.checkOut());
        BigDecimal discount = cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private static BigDecimal companyDiscount(String companyCode) {
        BigDecimal discount = companyCode == null ? null : CORPORATE_DISCOUNTS.get(companyCode);
        if (discount == null) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the corporate plan needs a known company code");
        }
        return discount;
    }
}
