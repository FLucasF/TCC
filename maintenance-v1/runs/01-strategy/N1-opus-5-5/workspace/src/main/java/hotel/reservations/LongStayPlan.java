package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

/** 7 nights or more, with 10% off. Everything comes back on cancellation. */
final class LongStayPlan implements RatePlanRules {

    static final int MINIMUM_NIGHTS = 7;
    static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    @Override
    public void checkEligibility(NewReservation request) {
        if (request.checkIn().plusDays(MINIMUM_NIGHTS).isAfter(request.checkOut())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the LONG_STAY plan needs at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price price(NewReservation request, Room room, Price seasonalPrice) {
        return seasonalPrice.withDiscount(DISCOUNT);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
