package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

final class StandardPricing implements RatePlanPricing {

    private static final BigDecimal HIGH_SEASON_SURCHARGE = new BigDecimal("0.20");

    @Override
    public void validate(NewReservation request, Room room) {
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = 0;
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = PriceCalculator.cents(BigDecimal.ZERO);
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }

    static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal nightlyRate = room.nightlyRate();
        if (isHighSeason(night)) {
            return PriceCalculator.cents(nightlyRate.multiply(BigDecimal.ONE.add(HIGH_SEASON_SURCHARGE)));
        }
        return PriceCalculator.cents(nightlyRate);
    }

    static boolean isHighSeason(LocalDate date) {
        Month month = date.getMonth();
        return month == Month.JANUARY
                || month == Month.FEBRUARY
                || month == Month.JULY
                || (month == Month.DECEMBER && date.getDayOfMonth() >= 15);
    }
}
