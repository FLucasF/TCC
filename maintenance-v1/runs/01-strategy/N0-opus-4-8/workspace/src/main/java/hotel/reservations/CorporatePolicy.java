package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * The corporate rate: a company that has a deal with the inn gets a discount on the nights. The guest
 * gives the company code when booking; an unknown or missing code is not allowed. Cancelling gives
 * everything back, like the standard plan.
 */
final class CorporatePolicy implements RatePlanPolicy {

    /** The companies with a deal, by code, each with the discount agreed on the nights. */
    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new HotelException(
                    ErrorCode.INVALID_REQUEST, "the corporate rate needs the code of a company with a deal");
        }
    }

    @Override
    public Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator pricing) {
        BigDecimal subtotal = pricing.nightsSubtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.cents(subtotal.multiply(COMPANY_DISCOUNTS.get(companyCode)));
        return new Price(PriceCalculator.nights(checkIn, checkOut), subtotal, discount, subtotal.subtract(discount));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
