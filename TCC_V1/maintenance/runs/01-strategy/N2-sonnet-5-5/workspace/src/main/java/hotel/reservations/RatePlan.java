package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new PercentageDiscountRule(BigDecimal.ZERO, 1, PercentageDiscountRule.EVERYTHING_BACK)),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new PercentageDiscountRule(new BigDecimal("0.15"), 1, paid -> PriceCalculator.cents(BigDecimal.ZERO))),
    /** A company agreement: the company's discount, chosen by the company code; everything comes back. */
    CORPORATE(new CorporateRule()),
    /** Friday to Sunday for a closed price all year; half comes back on cancellation. */
    WEEKEND_PACKAGE(new WeekendPackageRule()),
    /** 7 nights or more, 10% off; everything comes back. */
    LONG_STAY(new PercentageDiscountRule(new BigDecimal("0.10"), 7, PercentageDiscountRule.EVERYTHING_BACK));

    private final RatePlanRule rule;

    RatePlan(RatePlanRule rule) {
        this.rule = rule;
    }

    Price price(Room room, NewReservation request, Price seasonal) {
        return rule.price(room, request, seasonal);
    }

    BigDecimal refund(BigDecimal amountPaid) {
        return rule.refund(amountPaid);
    }
}
