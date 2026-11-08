package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class LongStayRatePlan implements RatePlanStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    @Override
    public void validate(NewReservation request, Room room) {
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < 7) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "long stay requires at least 7 nights");
        }
    }

    @Override
    public Price calculatePrice(NewReservation request, Room room) {
        int nights = 0;
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = request.checkIn(); night.isBefore(request.checkOut()); night = night.plusDays(1)) {
            subtotal = subtotal.add(PriceCalculator.nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
