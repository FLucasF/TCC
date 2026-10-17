package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class StandardStrategy implements RatePlanStrategy {

    private final PriceCalculator priceCalculator;

    StandardStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = 0;
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(priceCalculator.nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = PriceCalculator.cents(BigDecimal.ZERO);
        return new Price(nights, subtotal, discount, subtotal);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights) {
    }
}
