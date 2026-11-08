package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Corporate agreement: a discount based on the company code. Full refund on cancellation. */
final class CorporateRatePlanStrategy implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private final PriceCalculator priceCalculator;

    CorporateRatePlanStrategy(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(NewReservation request, Room room) {
        String companyCode = request.companyCode();
        if (companyCode == null || companyCode.isEmpty()) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "company code is required for corporate rate plan");
        }
        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "unknown company code: " + companyCode);
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        throw new UnsupportedOperationException("calculatePrice requires company code");
    }

    Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        return priceCalculator.calculateByNightlyRate(room, checkIn, checkOut, discountRate);
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
