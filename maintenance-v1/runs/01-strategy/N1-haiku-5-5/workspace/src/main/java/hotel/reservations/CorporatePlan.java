package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

final class CorporatePlan implements RatePlanRules {

    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void checkEligibility(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (companyCode == null || !DISCOUNT_BY_COMPANY.containsKey(companyCode)) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the corporate rate needs the code of a partner company");
        }
    }

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        return PriceCalculator.stay(room, checkIn, checkOut, DISCOUNT_BY_COMPANY.get(companyCode));
    }
}
