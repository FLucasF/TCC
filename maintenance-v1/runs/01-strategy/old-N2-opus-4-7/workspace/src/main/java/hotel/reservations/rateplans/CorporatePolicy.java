package hotel.reservations.rateplans;

import hotel.reservations.ErrorCode;
import hotel.reservations.HotelException;
import hotel.reservations.NewReservation;
import hotel.reservations.Price;
import hotel.reservations.PriceCalculator;
import hotel.rooms.Room;
import java.math.BigDecimal;
import java.util.Map;

final class CorporatePolicy implements RatePlanPolicy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(NewReservation request, Room room) {
        if (request.companyCode() == null || !COMPANY_DISCOUNTS.containsKey(request.companyCode())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "the corporate rate plan needs a known company code");
        }
    }

    @Override
    public Price price(NewReservation request, Room room, PriceCalculator pricing) {
        BigDecimal discount = COMPANY_DISCOUNTS.get(request.companyCode());
        return pricing.nightlyPlanPrice(room, request.checkIn(), request.checkOut(), discount);
    }
}
