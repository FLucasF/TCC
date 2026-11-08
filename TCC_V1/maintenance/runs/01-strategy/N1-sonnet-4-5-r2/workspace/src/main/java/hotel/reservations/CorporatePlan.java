package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

final class CorporatePlan implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(NewReservation request, Room room) {
        String code = request.companyCode();
        if (code == null || !COMPANY_DISCOUNTS.containsKey(code)) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "corporate plan requires a valid company code (TECNOSUL, AGROVALE, or FARMAVIDA)");
        }
    }

    @Override
    public BigDecimal calculateDiscount(
            BigDecimal subtotal, NewReservation request, Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(request.companyCode());
        return PriceCalculator.cents(subtotal.multiply(discountRate));
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
