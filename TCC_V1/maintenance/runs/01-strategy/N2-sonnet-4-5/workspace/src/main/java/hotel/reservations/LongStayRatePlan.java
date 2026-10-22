package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class LongStayRatePlan implements RatePlanStrategy {

    private static final int MINIMUM_NIGHTS = 7;
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");

    private final PriceCalculator priceCalculator;

    LongStayRatePlan(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (nights < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "long stay rate plan requires at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(priceCalculator.nightPrice(room, night));
        }
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
