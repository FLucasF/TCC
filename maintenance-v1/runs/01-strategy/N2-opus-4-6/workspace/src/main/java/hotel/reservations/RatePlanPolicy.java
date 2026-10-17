package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

interface RatePlanPolicy {

    default void validate(Room room, LocalDate checkIn, LocalDate checkOut) {}

    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateRefund(BigDecimal amountPaid);

    static RatePlanPolicy of(RatePlan ratePlan, String companyCode) {
        return switch (ratePlan) {
            case STANDARD -> new StandardPolicy();
            case NON_REFUNDABLE -> new NonRefundablePolicy();
            case CORPORATE -> new CorporatePolicy(companyCode);
            case WEEKEND_PACKAGE -> new WeekendPackagePolicy();
            case LONG_STAY -> new LongStayPolicy();
        };
    }
}
