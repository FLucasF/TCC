package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** The rate plans the guest can book under. Each plan's rules live in its own class. */
public enum RatePlan {
    STANDARD(new StandardPlan()),
    NON_REFUNDABLE(new NonRefundablePlan()),
    CORPORATE(new CorporatePlan()),
    WEEKEND_PACKAGE(new WeekendPackagePlan()),
    LONG_STAY(new LongStayPlan());

    private final RatePlanRules rules;

    RatePlan(RatePlanRules rules) {
        this.rules = rules;
    }

    void checkEligibility(NewReservation request) {
        rules.checkEligibility(request);
    }

    Price price(Room room, NewReservation request, PriceCalculator calculator) {
        return rules.price(room, request, calculator);
    }

    BigDecimal refund(BigDecimal amountPaid) {
        return rules.refund(amountPaid);
    }
}
