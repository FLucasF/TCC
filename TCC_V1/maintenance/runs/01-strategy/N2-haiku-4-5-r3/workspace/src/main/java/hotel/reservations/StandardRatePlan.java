package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

class StandardRatePlan implements RatePlanStrategy {

    static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");

    @Override
    public void validate(ReservationData data) throws HotelException {
        // Standard has no special validation beyond basic checks (done elsewhere)
    }

    @Override
    public Price calculate(Room room, ReservationData data) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = data.checkIn(); night.isBefore(data.checkOut()); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = cents(BigDecimal.ZERO);
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refundOnCancellation(Price price) {
        return price.total();
    }

    private BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal nightlyRate = room.nightlyRate();
        if (isHighSeason(night)) {
            return cents(nightlyRate.multiply(BigDecimal.ONE.add(HIGH_SEASON_SURCHARGE)));
        }
        return cents(nightlyRate);
    }

    private static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }

    protected static BigDecimal cents(BigDecimal value) {
        return PriceCalculator.cents(value);
    }
}
