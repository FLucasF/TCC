package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Long stay (7+ nights) with 10% discount; full refund on cancellation. */
final class LongStayRatePlan implements RatePlanStrategy {

    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    static final int MINIMUM_NIGHTS = 7;

    @Override
    public void validate(NewReservation request, Room room) {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "long stay plan requires at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price calculatePrice(NewReservation request, Room room) {
        int nights = 0;
        BigDecimal subtotal = PriceHelper.cents(BigDecimal.ZERO);
        for (LocalDate night = request.checkIn(); night.isBefore(request.checkOut()); night = night.plusDays(1)) {
            subtotal = subtotal.add(PriceHelper.nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = PriceHelper.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
