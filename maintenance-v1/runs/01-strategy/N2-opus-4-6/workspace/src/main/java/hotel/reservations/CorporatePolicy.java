package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

final class CorporatePolicy implements RatePlanPolicy {

    private static final Map<String, BigDecimal> COMPANIES = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private final BigDecimal discountRate;

    CorporatePolicy(String companyCode) {
        this.discountRate = companyCode == null ? null : COMPANIES.get(companyCode);
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (discountRate == null) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "a valid company code is required for the corporate plan");
        }
    }

    @Override
    public Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        int nights = PriceCalculator.countNights(checkIn, checkOut);
        BigDecimal subtotal = PriceCalculator.subtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
