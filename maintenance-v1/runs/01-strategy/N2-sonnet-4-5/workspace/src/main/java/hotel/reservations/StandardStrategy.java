package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class StandardStrategy implements RatePlanStrategy {

    @Override
    public void validate(NewReservation request, Room room) {
    }

    @Override
    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, NewReservation request) {
        int nights = 0;
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = PriceCalculator.cents(BigDecimal.ZERO);
        return new Price(nights, subtotal, discount, subtotal);
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }

    private BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal nightlyRate = room.nightlyRate();
        if (PriceCalculator.isHighSeason(night)) {
            return PriceCalculator.cents(nightlyRate.multiply(BigDecimal.ONE.add(PriceCalculator.HIGH_SEASON_SURCHARGE)));
        }
        return PriceCalculator.cents(nightlyRate);
    }
}
