package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** The rate plans; each one carries its own rules. A new plan is a new policy class and a new constant here. */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD(new StandardPlan()),
    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE(new NonRefundablePlan()),
    /** A discount per company, chosen by the company code; everything comes back on cancellation. */
    CORPORATE(new CorporatePlan()),
    /** Friday to Sunday for a closed price; half comes back on cancellation. */
    WEEKEND_PACKAGE(new WeekendPackagePlan()),
    /** 7 nights or more, 10% off; everything comes back on cancellation. */
    LONG_STAY(new LongStayPlan());

    private final RatePlanPolicy policy;

    RatePlan(RatePlanPolicy policy) {
        this.policy = policy;
    }

    Price price(Room room, NewReservation request, Price base) {
        return policy.price(room, request, base);
    }

    BigDecimal refund(BigDecimal amountPaid) {
        return policy.refund(amountPaid);
    }
}
