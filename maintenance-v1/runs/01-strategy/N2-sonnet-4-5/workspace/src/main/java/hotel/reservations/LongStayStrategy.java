package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

final class LongStayStrategy implements RatePlanStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    private static final int MINIMUM_NIGHTS = 7;

    @Override
    public void validate(NewReservation request, Room room) {
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
        if (nights < MINIMUM_NIGHTS) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "long stay rate plan requires at least " + MINIMUM_NIGHTS + " nights");
        }
    }

    @Override
    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, NewReservation request) {
        int nights = 0;
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(DISCOUNT_RATE));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
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
