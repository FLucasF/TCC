package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

final class CorporateStrategy implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(NewReservation request, Room room) {
        String companyCode = request.companyCode();
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "corporate rate plan requires a valid company code (TECNOSUL, AGROVALE, or FARMAVIDA)");
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
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(request.companyCode());
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(discountRate));
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
