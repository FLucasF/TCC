package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class StandardRatePlan implements RatePlanStrategy {

    @Override
    public void validate(NewReservation request, Room room) {}

    @Override
    public Price calculatePrice(NewReservation request, Room room) {
        int nights = 0;
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = request.checkIn(); night.isBefore(request.checkOut()); night = night.plusDays(1)) {
            subtotal = subtotal.add(PriceCalculator.nightPrice(room, night));
            nights++;
        }
        return new Price(nights, subtotal, PriceCalculator.cents(BigDecimal.ZERO), subtotal);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
