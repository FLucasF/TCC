package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Company partnership with discounts per company; full refund on cancellation. */
final class CorporateRatePlan implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(NewReservation request, Room room) {
        if (request.companyCode() == null || !COMPANY_DISCOUNTS.containsKey(request.companyCode())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "corporate plan requires a valid company code (TECNOSUL, AGROVALE, or FARMAVIDA)");
        }
    }

    @Override
    public Price calculatePrice(NewReservation request, Room room) {
        int nights = 0;
        BigDecimal subtotal = PriceHelper.cents(BigDecimal.ZERO);
        for (LocalDate night = request.checkIn(); night.isBefore(request.checkOut()); night = night.plusDays(1)) {
            subtotal = subtotal.add(PriceHelper.nightPrice(room, night));
            nights++;
        }
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(request.companyCode());
        BigDecimal discount = PriceHelper.cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.amountPaid();
    }
}
