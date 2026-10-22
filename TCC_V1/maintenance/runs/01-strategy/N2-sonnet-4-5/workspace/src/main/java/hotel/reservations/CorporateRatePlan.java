package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

final class CorporateRatePlan implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private final PriceCalculator priceCalculator;

    CorporateRatePlan(PriceCalculator priceCalculator) {
        this.priceCalculator = priceCalculator;
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "corporate rate plan requires a company code");
        }
        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new HotelException(ErrorCode.INVALID_REQUEST, "unknown company code: " + companyCode);
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        BigDecimal subtotal = PriceCalculator.cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(priceCalculator.nightPrice(room, night));
        }
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
