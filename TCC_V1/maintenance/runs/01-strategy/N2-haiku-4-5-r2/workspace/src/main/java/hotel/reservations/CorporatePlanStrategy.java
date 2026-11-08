package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

final class CorporatePlanStrategy implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private final PriceCalculator priceCalculator;

    CorporatePlanStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(NewReservation request) {
        if (request.companyCode() == null || request.companyCode().isBlank()) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "corporate rate requires a company code");
        }
        String code = request.companyCode().toUpperCase();
        if (!COMPANY_DISCOUNTS.containsKey(code)) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "company code '" + request.companyCode() + "' is not registered for corporate rates");
        }
    }

    @Override
    public Price calculatePrice(Room room, NewReservation request) {
        String code = request.companyCode().toUpperCase();
        BigDecimal discount = COMPANY_DISCOUNTS.get(code);
        return priceCalculator.nightlyPrice(room, request.checkIn(), request.checkOut(), discount);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
