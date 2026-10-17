package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class NonRefundableStrategy implements RatePlanStrategy {

    static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

    private final PriceCalculator priceCalculator;

    NonRefundableStrategy(PriceCalculator priceCalculator) {
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
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights) {
    }
}
