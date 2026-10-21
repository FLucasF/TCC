package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Corporate agreement: discount by company, full refund on cancellation. */
final class CorporateRatePlanStrategy implements RatePlanStrategy {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "corporate rate plan requires a company code");
        }
        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "company code '" + companyCode + "' is not recognized");
        }
    }

    @Override
    public Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal subtotal = PriceCalculator.sumNights(room, checkIn, checkOut);
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(discountRate));
        return new Price(
                PriceCalculator.countNights(checkIn, checkOut), subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refundAmount(BigDecimal amountPaid) {
        return amountPaid;
    }
}
