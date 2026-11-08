package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class CorporatePlanStrategy implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = new HashMap<>();

    static {
        COMPANY_DISCOUNTS.put("TECNOSUL", new BigDecimal("0.12"));
        COMPANY_DISCOUNTS.put("AGROVALE", new BigDecimal("0.18"));
        COMPANY_DISCOUNTS.put("FARMAVIDA", new BigDecimal("0.25"));
    }

    private final PriceCalculator priceCalculator;

    CorporatePlanStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, Optional<String> companyCode) {
        if (companyCode.isEmpty() || !COMPANY_DISCOUNTS.containsKey(companyCode.get())) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST,
                    "company code must be one of: " + String.join(", ", COMPANY_DISCOUNTS.keySet()));
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, Optional<String> companyCode) {
        BigDecimal discountPercent = COMPANY_DISCOUNTS.get(companyCode.get());
        return priceCalculator.calculateWithDiscount(room, checkIn, checkOut, discountPercent);
    }

    @Override
    public BigDecimal calculateRefund(Price price, ReservationStatus status) {
        return status == ReservationStatus.PENDING ? PriceCalculator.cents(BigDecimal.ZERO) : price.total();
    }
}
