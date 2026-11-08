package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class LongStayPlan implements RatePlanPolicy {

    static final int MINIMUM_NIGHTS = 7;
    static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public Price price(Room room, NewReservation request, Price base) {
        if (base.nights() < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the long stay plan needs at least " + MINIMUM_NIGHTS + " nights");
        }
        return RatePlanPolicy.discounted(base, DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
