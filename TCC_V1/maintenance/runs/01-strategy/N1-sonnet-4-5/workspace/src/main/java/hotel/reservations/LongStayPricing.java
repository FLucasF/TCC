package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

final class LongStayPricing implements RatePlanPricing {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    private static final int MINIMUM_NIGHTS = 7;

    @Override
    public void validate(NewReservation request, Room room) {
        long nights = java.time.temporal.ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "long stay rate plan requires at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = 0;
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(StandardPricing.nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
