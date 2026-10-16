package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;

final class LongStayPolicy implements RatePlanPolicy {

    private static final int MINIMUM_NIGHTS = 7;
    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    private final PriceCalculator priceCalculator;

    LongStayPolicy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(NewReservation request, Room room) {
        PriceCalculator.NightlySubtotal nightly =
                priceCalculator.nightlySubtotal(room, request.checkIn(), request.checkOut());
        if (nightly.nights() < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the long-stay rate needs at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price calculate(Room room, NewReservation request) {
        PriceCalculator.NightlySubtotal nightly =
                priceCalculator.nightlySubtotal(room, request.checkIn(), request.checkOut());
        BigDecimal discount = PriceCalculator.cents(nightly.subtotal().multiply(DISCOUNT));
        return new Price(nightly.nights(), nightly.subtotal(), discount, nightly.subtotal().subtract(discount));
    }

    @Override
    public BigDecimal refund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
